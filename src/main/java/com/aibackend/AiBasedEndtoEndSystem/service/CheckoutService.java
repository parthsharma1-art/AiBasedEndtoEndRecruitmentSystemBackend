package com.aibackend.AiBasedEndtoEndSystem.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.controller.CheckoutController;
import com.aibackend.AiBasedEndtoEndSystem.controller.CheckoutController.CheckoutRequest;
import com.aibackend.AiBasedEndtoEndSystem.controller.CheckoutController.PaymentVerificationRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout.CheckoutStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan.SubscriptionPlanType;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan.SubscriptionStatus;
import com.aibackend.AiBasedEndtoEndSystem.repository.CheckoutRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.SubscriptionPlanRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckoutService {
    private final CheckoutRepository checkoutRepo;
    private final SubscriptionPlanRepository subscriptionRepo;
    private final CompanyProfileService companyProfileService;
    private final RecruiterService recruiterService;
    private final BrevoEmailService brevoEmailService;
    private final UniqueUtility uniqueUtility;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public List<Checkout> getCheckoutsForRecruiter(UserDTO user) {
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        Recruiter recruiter = recruiterService.getRecruiterById(user.getId());
        if (recruiter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found");
        }
        return checkoutRepo.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId());
    }

    public Checkout createCheckoutFromRequest(UserDTO user, CheckoutRequest request)
            throws Exception {
        Recruiter recruiter = recruiterService.getRecruiterById(user.getId());
        if (recruiter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found");
        }
        CompanyProfile companyProfile = companyProfileService.getCompanyProfileByRecruiterId(user.getId());
        if (companyProfile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company profile not found");
        }

        // 1. Resolve & enforce plan parameters on the backend (prevents client price tampering)
        SubscriptionPlanType planType = request.getType() != null ? request.getType() : SubscriptionPlanType.BASIC;
        Long enforcedPrice = planType.getDefaultPriceInPaise();
        Integer enforcedDuration = planType.getDefaultDurationDays();
        String description = StringUtils.hasText(request.getDescription()) 
                ? request.getDescription() 
                : planType.getDefaultDescription();

        // 2. Compute accurate start & end dates (handles active, expired, or non-existent plans)
        Instant now = Instant.now();
        Instant startDate;
        Instant endDate;
        SubscriptionPlan currentPlan = subscriptionRepo.findByRecruiterId(recruiter.getId()).orElse(null);

        if (currentPlan != null 
                && SubscriptionStatus.ACTIVE.equals(currentPlan.getStatus()) 
                && currentPlan.getEndDate() != null 
                && currentPlan.getEndDate().isAfter(now)) {
            // Already active with future end date: queue after current end date
            startDate = currentPlan.getEndDate().plus(1, ChronoUnit.SECONDS);
            endDate = currentPlan.getEndDate().plus(enforcedDuration, ChronoUnit.DAYS);
        } else {
            // No plan, or expired plan: start from right now
            startDate = now;
            endDate = now.plus(enforcedDuration, ChronoUnit.DAYS);
        }

        // 3. Create or reuse a valid pending checkout
        Checkout checkout = createOrReusePendingCheckout(recruiter, companyProfile, planType, enforcedPrice, enforcedDuration, description, startDate, endDate);

        // If checkout already has a valid Razorpay order id and amount matches, return it directly
        if (StringUtils.hasText(checkout.getRazorpayOrderId())) {
            return checkout;
        }

        // 4. Create Razorpay order
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject notes = new JSONObject();
        notes.put("checkoutId", checkout.getId());
        notes.put("recruiterId", checkout.getRecruiterId());
        notes.put("companyId", checkout.getCompanyId());
        notes.put("planType", planType.name());

        JSONObject options = new JSONObject();
        options.put("amount", enforcedPrice);
        options.put("currency", "INR");
        options.put("receipt", "chk_" + checkout.getId());
        options.put("notes", notes);

        Order order = client.orders.create(options);
        checkout.setRazorpayOrderId(order.get("id"));
        checkout.setStatus(CheckoutStatus.PENDING);
        checkout.setUpdatedAt(Instant.now());
        checkout.setUpdatedBy(recruiter.getId());
        return checkoutRepo.save(checkout);
    }

    private Checkout createOrReusePendingCheckout(Recruiter recruiter, CompanyProfile companyProfile,
            SubscriptionPlanType planType, Long priceInPaise, Integer durationDays, String description,
            Instant startDate, Instant endDate) {
        Instant now = Instant.now();
        // Check for any existing pending checkout
        Checkout existingCheckout = checkoutRepo
                .findFirstByRecruiterIdAndStatusOrderByEndDateDesc(recruiter.getId(), CheckoutStatus.PENDING)
                .orElse(null);

        if (existingCheckout != null) {
            // Check if existing checkout is recent (< 30 minutes) and for the exact same plan
            boolean isRecent = existingCheckout.getCreatedAt() != null 
                    && existingCheckout.getCreatedAt().isAfter(now.minus(30, ChronoUnit.MINUTES));
            boolean isSamePlan = planType.equals(existingCheckout.getType());

            if (isRecent && isSamePlan && StringUtils.hasText(existingCheckout.getRazorpayOrderId())) {
                log.info("Reusing active pending checkoutId={} for recruiterId={}", existingCheckout.getId(), recruiter.getId());
                return existingCheckout;
            }

            // Otherwise, expire the outdated or mismatched pending checkout
            existingCheckout.setStatus(CheckoutStatus.EXPIRED);
            existingCheckout.setUpdatedAt(now);
            existingCheckout.setUpdatedBy("system:expired_by_new_request");
            checkoutRepo.save(existingCheckout);
        }

        // Create fresh checkout
        Checkout checkout = new Checkout();
        checkout.setId(uniqueUtility.getNextNumber("CHECKOUT", "chk"));
        checkout.setRecruiterId(recruiter.getId());
        checkout.setCompanyId(companyProfile.getId());
        checkout.setCompanyName(companyProfile.getBasicSetting() != null ? companyProfile.getBasicSetting().getCompanyName() : "Company");
        checkout.setType(planType);
        checkout.setPriceInPaise(priceInPaise);
        checkout.setDurationDays(durationDays);
        checkout.setDescription(description);
        checkout.setStartDate(startDate);
        checkout.setEndDate(endDate);
        checkout.setStatus(CheckoutStatus.PENDING);
        checkout.setCreatedAt(now);
        checkout.setUpdatedAt(now);
        checkout.setCreatedBy(recruiter.getId());
        return checkoutRepo.save(checkout);
    }

    /**
     * Direct payment verification invoked synchronously by frontend after Razorpay modal completes.
     */
    public Checkout verifyAndCapturePayment(UserDTO user, PaymentVerificationRequest request) {
        if (!StringUtils.hasText(request.getRazorpayOrderId())
                || !StringUtils.hasText(request.getRazorpayPaymentId())
                || !StringUtils.hasText(request.getRazorpaySignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing payment verification parameters");
        }

        // 1. Verify Razorpay Signature: HMAC SHA256 of (orderId + "|" + paymentId) with keySecret
        String data = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        try {
            String expectedSignature = hmacSha256(data, keySecret);
            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8));

            if (!isValid) {
                log.error("Payment signature verification failed for orderId={}", request.getRazorpayOrderId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }
        } catch (Exception e) {
            log.error("Error verifying payment signature", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification error");
        }

        // 2. Mark payment as captured & activate subscription
        return markPaymentCaptured(
                request.getRazorpayOrderId(),
                request.getCheckoutId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                null,
                "frontend_verify:" + user.getId());
    }

    /**
     * Thread-safe payment capture handler for both direct verification and webhooks.
     */
    public synchronized Checkout markPaymentCaptured(String razorpayOrderId, String checkoutId, String razorpayPaymentId,
            String razorpaySignature, String razorpayInvoiceId, String source) {
        Checkout checkout = findCheckout(razorpayOrderId, checkoutId);
        boolean alreadySuccessful = CheckoutStatus.SUCCESS.equals(checkout.getStatus());
        if (alreadySuccessful) {
            log.info("Skipping duplicate payment success update for checkoutId={} orderId={} paymentId={}",
                    checkout.getId(), razorpayOrderId, razorpayPaymentId);
            return checkout;
        }

        checkout.setStatus(CheckoutStatus.SUCCESS);
        checkout.setRazorpayPaymentId(razorpayPaymentId);
        if (StringUtils.hasText(razorpaySignature)) {
            checkout.setRazorpaySignature(razorpaySignature);
        }
        if (StringUtils.hasText(razorpayInvoiceId)) {
            checkout.setRazorpayInvoiceId(razorpayInvoiceId);
        }
        checkout.setUpdatedAt(Instant.now());
        checkout.setUpdatedBy(source);
        Checkout saved = checkoutRepo.save(checkout);

        updateSubscription(saved);
        sendCheckoutSuccessEmail(saved);
        return saved;
    }

    public synchronized Checkout markPaymentFailed(String razorpayOrderId, String checkoutId, String razorpayPaymentId,
            String reason, String source) {
        Checkout checkout = findCheckout(razorpayOrderId, checkoutId);
        if (CheckoutStatus.SUCCESS.equals(checkout.getStatus())) {
            log.warn("Cannot mark successful checkoutId={} as failed", checkout.getId());
            return checkout;
        }
        checkout.setStatus(CheckoutStatus.FAILED);
        if (StringUtils.hasText(razorpayPaymentId)) {
            checkout.setRazorpayPaymentId(razorpayPaymentId);
        }
        checkout.setUpdatedAt(Instant.now());
        checkout.setUpdatedBy(source + (StringUtils.hasText(reason) ? (":" + reason) : ""));
        return checkoutRepo.save(checkout);
    }

    private Checkout findCheckout(String razorpayOrderId, String checkoutId) {
        if (StringUtils.hasText(checkoutId)) {
            Optional<Checkout> byId = checkoutRepo.findById(checkoutId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (!StringUtils.hasText(razorpayOrderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing checkoutId and orderId");
        }
        return checkoutRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checkout not found for order: " + razorpayOrderId));
    }

    private void updateSubscription(Checkout checkout) {
        Optional<SubscriptionPlan> existingOpt = subscriptionRepo.findByRecruiterId(checkout.getRecruiterId());
        Instant now = Instant.now();
        SubscriptionPlan sub;
        Instant effectiveStartDate;
        Instant effectiveEndDate;
        int durationDays = checkout.getDurationDays() != null ? checkout.getDurationDays() : 30;

        if (existingOpt.isPresent()) {
            sub = existingOpt.get();
            if (SubscriptionStatus.ACTIVE.equals(sub.getStatus()) 
                    && sub.getEndDate() != null 
                    && sub.getEndDate().isAfter(now)) {
                // Subscription is actively running; keep start date and extend end date
                effectiveStartDate = sub.getStartDate() != null ? sub.getStartDate() : now;
                effectiveEndDate = sub.getEndDate().plus(durationDays, ChronoUnit.DAYS);
            } else {
                // Subscription was expired or suspended; start clean from now
                effectiveStartDate = now;
                effectiveEndDate = now.plus(durationDays, ChronoUnit.DAYS);
            }
        } else {
            sub = new SubscriptionPlan();
            sub.setId(uniqueUtility.getNextNumber("SUBSCRIPTION", "sub"));
            effectiveStartDate = now;
            effectiveEndDate = now.plus(durationDays, ChronoUnit.DAYS);
        }

        sub.setRecruiterId(checkout.getRecruiterId());
        sub.setCompanyId(checkout.getCompanyId());
        sub.setCompanyName(checkout.getCompanyName());
        sub.setType(checkout.getType());
        sub.setDescription(checkout.getDescription());
        sub.setStartDate(effectiveStartDate);
        sub.setEndDate(effectiveEndDate);
        sub.setPriceInPaise(checkout.getPriceInPaise());
        sub.setDurationDays((int) Duration.between(effectiveStartDate, effectiveEndDate).toDays());
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setUpdatedAt(now);
        if (sub.getCreatedAt() == null) {
            sub.setCreatedAt(now);
        }
        subscriptionRepo.save(sub);
    }

    @Async
    public void sendCheckoutSuccessEmail(Checkout checkout) {
        try {
            if (checkout == null || !StringUtils.hasText(checkout.getRecruiterId())) {
                return;
            }
            Recruiter recruiter = recruiterService.getRecruiterById(checkout.getRecruiterId());
            if (recruiter == null || !StringUtils.hasText(recruiter.getEmail())) {
                log.warn("Skipping checkout email: recruiter/email missing for recruiterId={}", checkout.getRecruiterId());
                return;
            }
            String recruiterName = recruiter.getName() != null ? recruiter.getName() : "Recruiter";
            String companyName = checkout.getCompanyName() != null ? checkout.getCompanyName() : "";
            String planType = checkout.getType() != null ? checkout.getType().name() : "PLAN";
            brevoEmailService.sendCheckoutSuccessEmail(
                    recruiter.getEmail(),
                    recruiterName,
                    companyName,
                    checkout.getPriceInPaise(),
                    planType,
                    checkout.getStartDate(),
                    checkout.getEndDate());
        } catch (Exception e) {
            log.warn("Failed to trigger checkout success email for checkoutId={}: {}", checkout.getId(), e.getMessage());
        }
    }

    private static String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder(2 * rawHmac.length);
        for (byte b : rawHmac) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

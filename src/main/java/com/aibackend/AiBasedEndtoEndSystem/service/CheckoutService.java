package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.controller.CheckoutController;
import com.aibackend.AiBasedEndtoEndSystem.controller.CheckoutController.CheckoutRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout.CheckoutStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan;
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

    public Checkout createCheckoutFromRequest(UserDTO user, CheckoutController.CheckoutRequest request)
            throws Exception {
        Recruiter recruiter = recruiterService.getRecruiterById(user.getId());
        if (recruiter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found");
        }
        CompanyProfile companyProfile = companyProfileService.getCompanyProfileByRecruiterId(user.getId());
        if (companyProfile == null) {
            throw new RuntimeException("Company profile not found");
        }
        Checkout checkout = createCheckout(recruiter, companyProfile, request);

        SubscriptionPlan subscriptionPlan = subscriptionRepo.findByRecruiterId(recruiter.getId()).orElse(null);
        if (!ObjectUtils.isEmpty(subscriptionPlan)) {
            if (SubscriptionStatus.ACTIVE.equals(subscriptionPlan.getStatus())) {
                checkout.setStartDate(subscriptionPlan.getEndDate().plus(1, ChronoUnit.SECONDS));
                checkout.setEndDate(subscriptionPlan.getEndDate().plus(request.getDurationDays(), ChronoUnit.DAYS));
            }
        } else {
            checkout.setStartDate(Instant.now());
            checkout.setEndDate(Instant.now().plus(request.getDurationDays(), ChronoUnit.DAYS));
        }

        checkout.setType(request.getType());
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject notes = new JSONObject();
        notes.put("checkoutId", checkout.getId());
        notes.put("recruiterId", checkout.getRecruiterId());
        notes.put("companyId", checkout.getCompanyId());

        JSONObject options = new JSONObject();
        options.put("amount", request.getPriceInPaise());
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

    public Checkout createCheckout(Recruiter recruiter, CompanyProfile companyProfile, CheckoutRequest request) {
        Checkout existingCheckout = checkoutRepo
                .findFirstByRecruiterIdAndStatusOrderByEndDateDesc(recruiter.getId(), CheckoutStatus.PENDING)
                .orElse(null);
        if (!ObjectUtils.isEmpty(existingCheckout)) {
            return existingCheckout;
        }
        Checkout checkout = new Checkout();
        checkout.setId(uniqueUtility.getNextNumber("CHECKOUT", "chk"));
        checkout.setRecruiterId(recruiter.getId());
        checkout.setCompanyId(companyProfile.getId());
        checkout.setCompanyName(companyProfile.getBasicSetting().getCompanyName());
        checkout.setPriceInPaise(request.getPriceInPaise());
        checkout.setDurationDays(request.getDurationDays());
        checkout.setDescription(request.getDescription());
        checkout.setStatus(CheckoutStatus.IN_PROGRESS);
        checkout.setCreatedAt(Instant.now());
        checkout.setCreatedBy(recruiter.getId());
        return checkoutRepo.save(checkout);
    }

    public Checkout markPaymentCaptured(String razorpayOrderId, String checkoutId, String razorpayPaymentId,
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
        checkout.setUpdatedAt(Instant.now());
        checkout.setUpdatedBy(source);
        Checkout saved = checkoutRepo.save(checkout);
        updateSubscription(saved);
        sendCheckoutSuccessEmail(saved);
        return saved;
    }

    public Checkout markPaymentFailed(String razorpayOrderId, String checkoutId, String razorpayPaymentId,
            String reason,
            String source) {
        Checkout checkout = findCheckout(razorpayOrderId, checkoutId);
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
            throw new RuntimeException("Missing checkoutId and orderId");
        }
        return checkoutRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Checkout not found"));
    }

    private void updateSubscription(Checkout checkout) {
        Optional<SubscriptionPlan> existingOpt = subscriptionRepo.findByRecruiterId(checkout.getRecruiterId());
        Instant now = Instant.now();
        SubscriptionPlan sub = null;
        Instant effectiveEndDate = null;
        Instant startDate = null;
        if (existingOpt.isPresent()) {
            sub = existingOpt.get();
            if (SubscriptionStatus.ACTIVE.equals(sub.getStatus())) {
                startDate = sub.getStartDate();
                Instant endDate = sub.getEndDate();
                effectiveEndDate = endDate.plus(checkout.getDurationDays(), ChronoUnit.DAYS);
            } else {
                startDate = now;
                effectiveEndDate = now.plus(checkout.getDurationDays(), ChronoUnit.DAYS);
            }
        } else {
            sub = new SubscriptionPlan();
            sub.setId(uniqueUtility.getNextNumber("SUBSCRIPTION", "sub"));
            startDate = now;
            effectiveEndDate = now.plus(checkout.getDurationDays(), ChronoUnit.DAYS);
        }

        sub.setRecruiterId(checkout.getRecruiterId());
        sub.setCompanyId(checkout.getCompanyId());
        sub.setCompanyName(checkout.getCompanyName());
        sub.setType(checkout.getType());
        sub.setDescription(checkout.getDescription());
        sub.setStartDate(startDate);
        sub.setEndDate(effectiveEndDate);
        sub.setPriceInPaise(checkout.getPriceInPaise());
        sub.setDurationDays((int) Duration.between(startDate, effectiveEndDate).toDays());
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setUpdatedAt(now);
        if (sub.getCreatedAt() == null) {
            sub.setCreatedAt(now);
        }
        checkoutRepo.save(checkout);
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

}
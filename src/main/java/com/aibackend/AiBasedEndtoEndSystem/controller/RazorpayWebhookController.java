package com.aibackend.AiBasedEndtoEndSystem.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aibackend.AiBasedEndtoEndSystem.service.CheckoutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/webhook/razorpay")
public class RazorpayWebhookController {

    private final ObjectMapper objectMapper;
    private final CheckoutService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping()
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature) {
        log.info("Razorpay webhook received: {}", payload);
        try {
            if (!StringUtils.hasText(razorpaySignature)
                    || !verifyWebhookSignature(payload, razorpaySignature, webhookSecret)) {
                log.error("Invalid Razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("ok", false, "message", "Invalid signature"));
            }

            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode payment = root.path("payload").path("payment").path("entity");

            String orderId = textOrNull(payment.path("order_id"));
            String paymentId = textOrNull(payment.path("id"));
            String invoiceId = textOrNull(payment.path("invoice_id"));
            String checkoutId = textOrNull(payment.path("notes").path("checkoutId"));
            log.info("Razorpay webhook checkoutId={}", checkoutId);
            if (checkoutId == null) {
                checkoutId = textOrNull(payment.path("notes").path("checkout_id"));
            }

            log.info("Razorpay webhook event={} orderId={} paymentId={} checkoutId={} invoiceId={}",
                    event, orderId, paymentId, checkoutId, invoiceId);

            if ("payment.captured".equals(event)) {
                var saved = paymentService.markPaymentCaptured(orderId, checkoutId, paymentId, razorpaySignature,
                        invoiceId, "webhook");
                log.info("Webhook processed payment capture for checkoutId={} persistedInvoiceId={}",
                        saved.getId(), saved.getRazorpayInvoiceId());
                return ResponseEntity.ok(Map.of("ok", true, "message", "Payment captured processed"));
            }
            if ("payment.failed".equals(event)) {
                String reason = textOrNull(payment.path("error_description"));
                if (reason == null) {
                    reason = textOrNull(payment.path("description"));
                }
                paymentService.markPaymentFailed(orderId, checkoutId, paymentId, reason, "webhook");
                return ResponseEntity.ok(Map.of("ok", true, "message", "Payment failure processed"));
            }

            Map<String, Object> ignored = new HashMap<>();
            ignored.put("ok", true);
            ignored.put("message", "Event ignored");
            ignored.put("event", event);
            return ResponseEntity.ok(ignored);
        } catch (Exception e) {
            log.error("Error in Razorpay webhook processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "Webhook processing error"));
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText("");
        return text.isBlank() ? null : text;
    }

    private boolean verifyWebhookSignature(String payload, String actualSignature, String secret) throws Exception {
        String expectedSignature = hmacSha256(payload, secret);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                actualSignature.getBytes(StandardCharsets.UTF_8));
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

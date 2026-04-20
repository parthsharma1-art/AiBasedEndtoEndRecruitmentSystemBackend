package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan.SubscriptionPlanType;

import lombok.Data;

@Data
@Document(collection = "checkouts")
public class Checkout {
    @Id
    private String id;
    private String companyId;
    private String recruiterId;
    private String companyName;
    private Long priceInPaise;
    private Integer durationDays;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private SubscriptionPlanType type;
    private CheckoutStatus status;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String razorpayInvoiceId;

    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;


    public enum CheckoutStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        CANCELLED,
        EXPIRED
    }

}

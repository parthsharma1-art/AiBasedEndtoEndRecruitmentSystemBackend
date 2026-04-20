package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "subscription_plans")
@Data
public class SubscriptionPlan {

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

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    private SubscriptionPlanType type;
    private SubscriptionStatus status;

    public enum SubscriptionPlanType {
        GRACE_PERIOD,
        BASIC,
        STANDARD,
        PREMIUM,
        FREE;
    }

    public enum SubscriptionStatus {
        ACTIVE,
        TRIAL,
        EXPIRED,
        CANCELLED,
        SUSPENDED
    }
}
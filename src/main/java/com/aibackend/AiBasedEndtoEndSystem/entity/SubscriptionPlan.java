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
        GRACE_PERIOD(0L, 7, "Grace Period - 7 days"),
        BASIC(2000L, 30, "Basic Plan - 30 days"),
        STANDARD(5000L, 90, "Standard Plan - 90 days"),
        PREMIUM(19900L, 365, "Premium Plan - 365 days"),
        FREE(0L, 0, "Free Plan");

        private final Long defaultPriceInPaise;
        private final Integer defaultDurationDays;
        private final String defaultDescription;

        SubscriptionPlanType(Long defaultPriceInPaise, Integer defaultDurationDays, String defaultDescription) {
            this.defaultPriceInPaise = defaultPriceInPaise;
            this.defaultDurationDays = defaultDurationDays;
            this.defaultDescription = defaultDescription;
        }

        public Long getDefaultPriceInPaise() {
            return defaultPriceInPaise;
        }

        public Integer getDefaultDurationDays() {
            return defaultDurationDays;
        }

        public String getDefaultDescription() {
            return defaultDescription;
        }
    }

    public enum SubscriptionStatus {
        ACTIVE,
        TRIAL,
        EXPIRED,
        CANCELLED,
        SUSPENDED
    }
}

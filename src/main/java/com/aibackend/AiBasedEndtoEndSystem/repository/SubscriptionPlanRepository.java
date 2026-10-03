package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan.SubscriptionStatus;

public interface SubscriptionPlanRepository extends MongoRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByRecruiterId(String recruiterId);
    List<SubscriptionPlan> findByStatusAndEndDateBefore(SubscriptionStatus status, Instant endDate);
}

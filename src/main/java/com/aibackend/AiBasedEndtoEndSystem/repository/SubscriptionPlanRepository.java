package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan;

public interface SubscriptionPlanRepository extends MongoRepository<SubscriptionPlan, String> {
    Optional<SubscriptionPlan> findByRecruiterId(String recruiterId);
}
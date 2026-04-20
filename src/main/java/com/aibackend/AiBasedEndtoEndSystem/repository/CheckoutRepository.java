package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout.CheckoutStatus;

public interface CheckoutRepository extends MongoRepository<Checkout, String> {
    Optional<Checkout> findByRazorpayOrderId(String razorpayOrderId);
    List<Checkout> findByRecruiterIdOrderByCreatedAtDesc(String recruiterId);
    Optional<Checkout> findFirstByRecruiterIdAndStatusOrderByEndDateDesc(String recruiterId, CheckoutStatus status);
    Optional<Checkout> findFirstByRecruiterIdAndStatusAndIdNotOrderByEndDateDesc(
            String recruiterId, CheckoutStatus status, String id);
}
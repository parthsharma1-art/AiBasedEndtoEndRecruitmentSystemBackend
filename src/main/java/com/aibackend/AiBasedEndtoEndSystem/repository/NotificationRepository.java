package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecruiterIdOrderByCreatedAtDesc(String recruiterId);

    List<Notification> findByCandidateIdOrderByCreatedAtDesc(String candidateId);

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId);

    List<Notification> findByReceiverIdAndReadFalse(String receiverId);
}

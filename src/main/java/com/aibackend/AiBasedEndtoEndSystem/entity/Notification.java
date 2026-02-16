package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String title;
    private String message;
    private String senderId;
    private String receiverId;
    private String relativeId;
    private String recruiterId;
    private String candidateId;
    private Boolean read;
    private Chat.Source source;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

}
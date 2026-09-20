package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "notifications")
public class Notification {

    /** Identifies the domain this notification belongs to — used by the frontend for routing. */
    public enum NotificationType {
        CHAT,          // relativeId = chatId  → /chats/{chatId}
        JOB,           // relativeId = jobId   → /applied-jobs or /jobs/{jobId}
        AI_SCREENING   // relativeId = jobApplicationId or jobId
    }

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
    private NotificationType notificationType;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

}

package com.aibackend.AiBasedEndtoEndSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Supabase notification requests
 * Used to send notifications to recipients via Supabase real-time API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupabaseNotificationRequest {
    
    /**
     * ID of the notification recipient
     */
    private String recipientId;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message/content
     */
    private String message;
    
    /**
     * Type of notification (e.g., "message", "alert", "status_update", "job_application")
     */
    private String type;
    
    /**
     * ID of the related entity (e.g., chat ID, job posting ID, application ID)
     */
    private String relatedId;
    
    /**
     * Additional metadata as JSON string (optional)
     */
    private String metadata;
}

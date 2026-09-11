package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.SupabaseNotificationRequest;
import com.aibackend.AiBasedEndtoEndSystem.service.SupabaseNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing notifications via Supabase
 * Provides endpoints for sending, managing, and tracking real-time notifications
 */
@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private SupabaseNotificationService supabaseNotificationService;

    /**
     * Send a notification to a specific recipient
     * 
     * @param request Notification request containing recipient and message details
     * @return Success/failure response
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody SupabaseNotificationRequest request) {
        log.info("Sending notification to recipient: {}", request.getRecipientId());
        
        if (request.getRecipientId() == null || request.getRecipientId().isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient ID is required");
        }
        
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }

        try {
            Boolean success = supabaseNotificationService.sendNotification(
                    request.getRecipientId(),
                    request.getTitle() != null ? request.getTitle() : "Notification",
                    request.getMessage(),
                    request.getType() != null ? request.getType() : "alert",
                    request.getRelatedId(),
                    request.getMetadata()
            ).join();

            if (success) {
                return ResponseEntity.ok().body("Notification sent successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to send notification");
            }
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Send a message notification (simplified endpoint for messages)
     * 
     * @param recipientId ID of the recipient
     * @param senderId    ID of the sender
     * @param message     Message content
     * @return Success/failure response
     */
    @PostMapping("/send-message/{recipientId}")
    public ResponseEntity<?> sendMessageNotification(
            @PathVariable String recipientId,
            @RequestParam String senderId,
            @RequestParam String message) {
        
        log.info("Sending message notification from {} to {}", senderId, recipientId);

        try {
            Boolean success = supabaseNotificationService.sendMessageNotification(
                    recipientId,
                    senderId,
                    message
            ).join();

            if (success) {
                return ResponseEntity.ok().body("Message notification sent successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to send message notification");
            }
        } catch (Exception e) {
            log.error("Error sending message notification", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Send a status update notification
     * 
     * @param recipientId ID of the recipient
     * @param title       Notification title
     * @param message     Notification message
     * @param relatedId   ID of the related entity
     * @return Success/failure response
     */
    @PostMapping("/send-status-update/{recipientId}")
    public ResponseEntity<?> sendStatusUpdateNotification(
            @PathVariable String recipientId,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam String relatedId) {
        
        log.info("Sending status update notification to {}", recipientId);

        try {
            Boolean success = supabaseNotificationService.sendStatusUpdateNotification(
                    recipientId,
                    title,
                    message,
                    relatedId
            ).join();

            if (success) {
                return ResponseEntity.ok().body("Status update notification sent successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to send status update notification");
            }
        } catch (Exception e) {
            log.error("Error sending status update notification", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Mark a notification as read
     * 
     * @param notificationId ID of the notification to mark as read
     * @return Success/failure response
     */
    @PutMapping("/mark-as-read/{notificationId}")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        try {
            Boolean success = supabaseNotificationService.markAsRead(notificationId).join();

            if (success) {
                return ResponseEntity.ok().body("Notification marked as read");
            } else {
                return ResponseEntity.internalServerError().body("Failed to mark notification as read");
            }
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Delete a notification
     * 
     * @param notificationId ID of the notification to delete
     * @return Success/failure response
     */
    @DeleteMapping("/delete/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable String notificationId) {
        log.info("Deleting notification: {}", notificationId);

        try {
            Boolean success = supabaseNotificationService.deleteNotification(notificationId).join();

            if (success) {
                return ResponseEntity.ok().body("Notification deleted successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to delete notification");
            }
        } catch (Exception e) {
            log.error("Error deleting notification", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint for Supabase notification service
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        log.info("Checking notification service health");
        return ResponseEntity.ok().body("Notification service is running");
    }
}

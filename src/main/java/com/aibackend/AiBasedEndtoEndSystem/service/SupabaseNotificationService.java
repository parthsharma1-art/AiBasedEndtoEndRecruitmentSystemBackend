package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.config.SupabaseConfig;
import com.aibackend.AiBasedEndtoEndSystem.entity.Notification;
import com.aibackend.AiBasedEndtoEndSystem.repository.NotificationRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing real-time notifications via Supabase REST API.
 *
 * Security model:
 *  - All server-to-Supabase calls use the service_role key (bypasses RLS).
 *  - The frontend reads via anon key + a short-lived Supabase JWT issued by SupabaseTokenService.
 *  - The service_role key is NEVER sent to or logged for the frontend.
 */
@Service
@Slf4j
public class SupabaseNotificationService {

    /** Notification type constants. */
    public static final String TYPE_APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    public static final String TYPE_SHORTLISTED           = "SHORTLISTED";
    public static final String TYPE_REJECTED              = "REJECTED";
    public static final String TYPE_HIRED                 = "HIRED";
    public static final String TYPE_AI_SCREENING_RESULT   = "AI_SCREENING_RESULT";
    public static final String TYPE_INTERVIEW_SCHEDULED   = "INTERVIEW_SCHEDULED";
    public static final String TYPE_NEW_MESSAGE           = "NEW_MESSAGE";
    public static final String TYPE_JOB_POSTED            = "JOB_POSTED";
    public static final String TYPE_GENERAL               = "GENERAL";

    @Autowired
    private SupabaseConfig supabaseConfig;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UniqueUtility uniqueUtility;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String CONTENT_TYPE        = "application/json";
    private final OkHttpClient httpClient = new OkHttpClient();

    // -----------------------------------------------------------------------
    // PUBLIC API
    // -----------------------------------------------------------------------

    /**
     * Sends a notification to Supabase (real-time delivery) and saves a
     * corresponding record in MongoDB (history/pagination).
     */
    public CompletableFuture<Boolean> sendNotification(
            String recipientId, String title, String message,
            String type, String relatedId, String metadata) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Save to MongoDB (history / REST API fallback)
                Notification mongoNotification = new Notification();
                mongoNotification.setId(uniqueUtility.getNextNumber("NOTIFICATION", "notification"));
                mongoNotification.setTitle(title);
                mongoNotification.setMessage(message);
                mongoNotification.setReceiverId(recipientId);
                mongoNotification.setRelativeId(relatedId);
                mongoNotification.setRead(false);
                mongoNotification.setCreatedAt(Instant.now());
                mongoNotification.setUpdatedAt(Instant.now());
                mongoNotification.setFailureReason(null);

                try {
                    notificationRepository.save(mongoNotification);
                    log.debug("Notification saved to MongoDB: {}", mongoNotification.getId());
                } catch (Exception e) {
                    log.error("Error saving notification to MongoDB", e);
                    mongoNotification.setFailureReason("MongoDB save failed: " + e.getMessage());
                }

                // 2. Send to Supabase (real-time delivery)
                if (!supabaseConfig.isSupabaseEnabled()) {
                    log.debug("Supabase is disabled. Notification saved to MongoDB only.");
                    return true;
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("recipient_id", recipientId);
                payload.put("title", title);
                payload.put("message", message);
                payload.put("type", type != null ? type : TYPE_GENERAL);
                payload.put("is_read", false);
                payload.put("created_at", Instant.now().toString());
                payload.put("mongodb_notification_id", mongoNotification.getId());

                if (metadata != null && !metadata.isEmpty()) {
                    try {
                        payload.put("metadata", objectMapper.readTree(metadata));
                    } catch (Exception e) {
                        log.warn("Failed to parse metadata JSON: {}", e.getMessage());
                    }
                }

                boolean supabaseSuccess = insertToSupabase(NOTIFICATIONS_TABLE, payload);
                if (supabaseSuccess) {
                    log.info("Notification delivered via Supabase Realtime to recipient: {}", recipientId);
                } else {
                    log.warn("Supabase delivery failed for: {}. MongoDB record still saved.", recipientId);
                }
                return supabaseSuccess;
            } catch (Exception e) {
                log.error("Error sending notification to recipient: {}", recipientId, e);
                return false;
            }
        });
    }

    /**
     * Sends a notification with a structured metadata map (preferred for rich notifications).
     */
    public CompletableFuture<Boolean> sendNotificationAsync(
            String recipientId, String title, String message,
            String type, Map<String, String> metadataMap) {

        String metadataJson = null;
        if (metadataMap != null && !metadataMap.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadataMap);
            } catch (Exception e) {
                log.warn("Failed to serialize metadata for {}: {}", recipientId, e.getMessage());
            }
        }
        return sendNotification(recipientId, title, message, type, null, metadataJson);
    }

    /** Simplified notification for chat message events. */
    public CompletableFuture<Boolean> sendMessageNotification(
            String recipientId, String senderId, String message) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("senderId", senderId);
        return sendNotificationAsync(recipientId, "New Message", message, TYPE_NEW_MESSAGE, metadata);
    }

    /** Send notification for application status updates. */
    public CompletableFuture<Boolean> sendStatusUpdateNotification(
            String recipientId, String title, String message, String relatedId) {
        return sendNotification(recipientId, title, message, TYPE_GENERAL, relatedId, null);
    }

    /** Send bulk notifications to multiple recipients (fire-and-forget). */
    public CompletableFuture<Integer> sendBulkNotifications(
            List<String> recipientIds, String title, String message,
            String type, String relatedId) {

        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            for (String recipientId : recipientIds) {
                try {
                    Boolean success = sendNotification(recipientId, title, message, type, relatedId, null).join();
                    if (Boolean.TRUE.equals(success)) successCount++;
                } catch (Exception e) {
                    log.error("Error sending bulk notification to {}", recipientId, e);
                }
            }
            return successCount;
        });
    }

    /**
     * Marks a notification as read in Supabase and MongoDB.
     * Uses service_role key to bypass RLS.
     */
    public CompletableFuture<Boolean> markAsRead(String notificationId) {
        if (!supabaseConfig.isSupabaseEnabled()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Update MongoDB
                try {
                    Notification n = notificationRepository.findById(notificationId).orElse(null);
                    if (n != null) {
                        n.setRead(true);
                        n.setUpdatedAt(Instant.now());
                        notificationRepository.save(n);
                    }
                } catch (Exception e) {
                    log.error("Error updating MongoDB notification read status: {}", notificationId, e);
                }

                // Update Supabase
                Map<String, Object> updatePayload = new HashMap<>();
                updatePayload.put("is_read", true);

                String url = supabaseConfig.getSupabaseUrl()
                        + "/rest/v1/" + NOTIFICATIONS_TABLE
                        + "?mongodb_notification_id=eq." + notificationId;

                String json = objectMapper.writeValueAsString(updatePayload);
                RequestBody body = RequestBody.create(json, MediaType.parse(CONTENT_TYPE));
                Request request = new Request.Builder()
                        .url(url).patch(body)
                        .addHeader("apikey", supabaseConfig.getServiceRoleKey())
                        .addHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                        .addHeader("Content-Type", CONTENT_TYPE)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("Supabase markAsRead failed: {} - {}",
                                response.code(),
                                response.body() != null ? response.body().string() : "");
                    }
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                log.error("Error marking notification as read: {}", notificationId, e);
                return false;
            }
        });
    }

    /** Deletes a notification from Supabase and MongoDB. */
    public CompletableFuture<Boolean> deleteNotification(String notificationId) {
        if (!supabaseConfig.isSupabaseEnabled()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                try { notificationRepository.deleteById(notificationId); }
                catch (Exception e) { log.error("Error deleting from MongoDB: {}", notificationId, e); }

                String url = supabaseConfig.getSupabaseUrl()
                        + "/rest/v1/" + NOTIFICATIONS_TABLE
                        + "?mongodb_notification_id=eq." + notificationId;

                Request request = new Request.Builder()
                        .url(url).delete()
                        .addHeader("apikey", supabaseConfig.getServiceRoleKey())
                        .addHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        log.warn("Supabase deleteNotification failed: {} - {}",
                                response.code(),
                                response.body() != null ? response.body().string() : "");
                    }
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                log.error("Error deleting notification: {}", notificationId, e);
                return false;
            }
        });
    }

    // -----------------------------------------------------------------------
    // QUERY HELPERS (MongoDB)
    // -----------------------------------------------------------------------

    public List<Notification> getNotificationsForRecipient(String recipientId) {
        try { return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(recipientId); }
        catch (Exception e) { log.error("Error retrieving notifications for {}", recipientId, e); return new ArrayList<>(); }
    }

    public List<Notification> getUnreadNotificationsForRecipient(String recipientId) {
        try { return notificationRepository.findByReceiverIdAndReadFalse(recipientId); }
        catch (Exception e) { log.error("Error retrieving unread notifications for {}", recipientId, e); return new ArrayList<>(); }
    }

    public Notification getNotification(String notificationId) {
        try { return notificationRepository.findById(notificationId).orElse(null); }
        catch (Exception e) { log.error("Error retrieving notification {}", notificationId, e); return null; }
    }

    // -----------------------------------------------------------------------
    // PRIVATE HELPERS
    // -----------------------------------------------------------------------

    /**
     * Inserts a row using service_role key (bypasses RLS).
     */
    private boolean insertToSupabase(String table, Map<String, Object> payload) {
        try {
            String url = supabaseConfig.getSupabaseUrl() + "/rest/v1/" + table;
            String json = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(json, MediaType.parse(CONTENT_TYPE));
            Request request = new Request.Builder()
                    .url(url).post(body)
                    .addHeader("apikey", supabaseConfig.getServiceRoleKey())
                    .addHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                    .addHeader("Content-Type", CONTENT_TYPE)
                    .addHeader("Prefer", "return=minimal")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Supabase insert failed [{}]: {} - {}",
                            table, response.code(),
                            response.body() != null ? response.body().string() : "");
                    return false;
                }
                return true;
            }
        } catch (IOException e) {
            log.error("IO error during Supabase insert [{}]: {}", table, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error during Supabase insert [{}]: {}", table, e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        if (httpClient.connectionPool() != null) httpClient.connectionPool().evictAll();
    }
}

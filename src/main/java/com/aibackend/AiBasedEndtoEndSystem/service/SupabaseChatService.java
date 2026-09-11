package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.config.SupabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Broadcasts chat events via Supabase Realtime Broadcast API.
 *
 * Architecture:
 *  - Chat messages are stored in MongoDB (unchanged, AES-encrypted).
 *  - When a new message is saved, this service broadcasts a lightweight signal
 *    on a Supabase Realtime Broadcast channel (chat:{chatId}).
 *  - The React frontend receives the broadcast and calls fetchChats() from Spring Boot.
 *  - No chat data is stored in Supabase. Zero data duplication.
 *
 * Broadcast endpoint: POST {supabaseUrl}/realtime/v1/api/broadcast
 * Uses service_role key for authentication.
 */
@Service
@Slf4j
public class SupabaseChatService {

    @Autowired
    private SupabaseConfig supabaseConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String CONTENT_TYPE = "application/json";

    /**
     * Broadcasts a "new_message" event on channel "chat:{chatId}".
     * This is fire-and-forget: failures are logged but do not affect the chat response.
     *
     * @param chatId     MongoDB chat document ID
     * @param senderId   MongoDB ID of the message sender
     * @param senderRole "RECRUITER" or "CANDIDATE"
     */
    public CompletableFuture<Boolean> broadcastNewMessage(
            String chatId, String senderId, String senderRole) {

        if (!supabaseConfig.isSupabaseEnabled()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Supabase Realtime Broadcast REST API
                // POST {project_url}/realtime/v1/api/broadcast
                // Body: { "messages": [{ "topic": "chat:{chatId}", "event": "new_message", "payload": {...} }] }
                String url = supabaseConfig.getSupabaseUrl() + "/realtime/v1/api/broadcast";

                Map<String, Object> payload = new HashMap<>();
                payload.put("chatId", chatId);
                payload.put("senderId", senderId);
                payload.put("senderRole", senderRole);
                payload.put("timestamp", System.currentTimeMillis());

                Map<String, Object> message = new HashMap<>();
                message.put("topic", "chat:" + chatId);
                message.put("event", "new_message");
                message.put("payload", payload);

                Map<String, Object> body = new HashMap<>();
                body.put("messages", java.util.List.of(message));

                String json = objectMapper.writeValueAsString(body);
                RequestBody requestBody = RequestBody.create(json, MediaType.parse(CONTENT_TYPE));

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .addHeader("apikey", supabaseConfig.getServiceRoleKey())
                        .addHeader("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey())
                        .addHeader("Content-Type", CONTENT_TYPE)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Chat broadcast sent for chatId={}, sender={}", chatId, senderId);
                        return true;
                    } else {
                        log.warn("Chat broadcast failed for chatId={}: {} - {}",
                                chatId, response.code(),
                                response.body() != null ? response.body().string() : "");
                        return false;
                    }
                }
            } catch (Exception e) {
                log.error("Error broadcasting chat message for chatId={}: {}", chatId, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Cleans up OkHttp connection pool on shutdown.
     */
    public void shutdown() {
        if (httpClient.connectionPool() != null) httpClient.connectionPool().evictAll();
    }
}

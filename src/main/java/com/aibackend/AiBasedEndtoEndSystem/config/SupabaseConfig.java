package com.aibackend.AiBasedEndtoEndSystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Supabase Configuration for real-time notifications and chat broadcast.
 *
 * Key separation (security):
 *  - serviceRoleKey  : backend-only secret that bypasses Row Level Security.
 *                      NEVER send to frontend or log.
 *  - supabaseJwtSecret: signs short-lived JWTs so the frontend can authenticate
 *                       Supabase Realtime subscriptions without Supabase Auth.
 */
@Configuration
@Slf4j
public class SupabaseConfig {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    /**
     * Supabase service_role (secret) key.
     * Backend REST API calls use this to bypass Row Level Security.
     * NEVER expose this to the frontend.
     */
    @Value("${supabase.service-role-key:}")
    private String serviceRoleKey;

    /**
     * JWT Secret from Supabase Dashboard (Settings -> API -> JWT Secret).
     * Used to sign short-lived tokens for frontend Realtime authorization.
     */
    @Value("${supabase.jwt-secret:}")
    private String supabaseJwtSecret;

    @Value("${supabase.enabled:true}")
    private boolean supabaseEnabled;

    /**
     * Validates Supabase configuration on application startup.
     */
    public void init() {
        if (supabaseEnabled) {
            if (isEmpty(supabaseUrl)) {
                log.warn("Supabase URL is not configured. Real-time features may not work.");
            }
            if (isEmpty(serviceRoleKey)) {
                log.warn("Supabase service_role key is not configured. Backend notification writes will fail.");
            }
            if (isEmpty(supabaseJwtSecret)) {
                log.warn("Supabase JWT Secret is not configured. Frontend Realtime token issuance will fail.");
            }
            if (!isEmpty(supabaseUrl) && !isEmpty(serviceRoleKey) && !isEmpty(supabaseJwtSecret)) {
                log.info("Supabase configuration initialized successfully (url={})", supabaseUrl);
            }
        } else {
            log.info("Supabase integration is disabled");
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    /**
     * Returns the service_role (secret) key for backend Supabase REST calls.
     * Use for INSERT/UPDATE/DELETE operations that must bypass RLS.
     */
    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    /**
     * Returns the JWT Secret for signing Supabase Realtime tokens.
     */
    public String getSupabaseJwtSecret() {
        return supabaseJwtSecret;
    }

    public boolean isSupabaseEnabled() {
        return supabaseEnabled;
    }
}

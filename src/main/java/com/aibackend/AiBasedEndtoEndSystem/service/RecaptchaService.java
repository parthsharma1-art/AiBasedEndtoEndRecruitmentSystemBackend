package com.aibackend.AiBasedEndtoEndSystem.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * RecaptchaService – verifies a Google reCAPTCHA Enterprise token.
 *
 * How it works:
 *   1. Frontend calls  grecaptcha.enterprise.execute(SITE_KEY, {action})
 *      which returns a short-lived token (valid ~2 minutes).
 *   2. Frontend sends that token to the backend along with the form data.
 *   3. This service POSTs to the reCAPTCHA Enterprise Assessment API.
 *   4. Google returns a risk score (0.0 = bot, 1.0 = human).
 *   5. We reject requests with score < 0.5 (configurable via recaptcha.min-score).
 *
 * No Google Cloud SDK needed – uses OkHttp (already in pom.xml) + Gson.
 */
@Service
@Slf4j
public class RecaptchaService {

    private static final String ASSESSMENT_URL =
            "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s";

    /** Your Google Cloud project ID */
    @Value("${recaptcha.project-id:aibasedrecruitmentsystem}")
    private String projectId;

    /** The reCAPTCHA Enterprise SITE key (public, used on frontend too) */
    @Value("${recaptcha.site-key:6Ld1cdstAAAAAJ3P_CdxKQ1KCm_kE7BMoRUZOgYA}")
    private String siteKey;

    /** Your Google Cloud API key (server-side only – keep secret!) */
    @Value("${recaptcha.api-key:${RECAPTCHA_API_KEY:}}")
    private String apiKey;

    /** Minimum risk score to allow (0.0–1.0). Default 0.5 */
    @Value("${recaptcha.min-score:0.5}")
    private float minScore;

    /** Set false in dev to skip verification entirely */
    @Value("${recaptcha.enabled:true}")
    private boolean enabled;

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    /**
     * Verify a reCAPTCHA token and check the risk score.
     *
     * @param token         The token from the frontend
     * @param expectedAction The action string (e.g. "LOGIN", "SIGNUP")
     * @return true if the token is valid and the score is acceptable
     */
    public boolean verify(String token, String expectedAction) {
        if (!enabled) {
            log.info("reCAPTCHA verification is disabled (recaptcha.enabled=false)");
            return true;
        }

        if (!StringUtils.hasText(token)) {
            log.warn("reCAPTCHA token is empty – rejecting");
            return false;
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("RECAPTCHA_API_KEY env var not set – skipping score check (allow-all)");
            return true; // Fail open if not configured (you can change to false for strict)
        }

        try {
            // Build request body per Enterprise API spec
            JsonObject event = new JsonObject();
            event.addProperty("token", token);
            event.addProperty("siteKey", siteKey);
            event.addProperty("expectedAction", expectedAction);

            JsonObject body = new JsonObject();
            body.add("event", event);

            String url = String.format(ASSESSMENT_URL, projectId, apiKey);
            RequestBody reqBody = RequestBody.create(
                    gson.toJson(body),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("reCAPTCHA API returned HTTP {}", response.code());
                    return false;
                }

                String responseBody = response.body().string();
                log.debug("reCAPTCHA assessment response: {}", responseBody);
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);

                // Check token validity
                if (json.has("tokenProperties")) {
                    JsonObject tokenProps = json.getAsJsonObject("tokenProperties");
                    boolean valid = tokenProps.has("valid") && tokenProps.get("valid").getAsBoolean();
                    if (!valid) {
                        String reason = tokenProps.has("invalidReason")
                                ? tokenProps.get("invalidReason").getAsString() : "UNKNOWN";
                        log.warn("reCAPTCHA token invalid: {}", reason);
                        return false;
                    }

                    // Verify expected action matches
                    if (tokenProps.has("action")) {
                        String action = tokenProps.get("action").getAsString();
                        if (!action.equalsIgnoreCase(expectedAction)) {
                            log.warn("reCAPTCHA action mismatch: expected={} got={}", expectedAction, action);
                            return false;
                        }
                    }
                }

                // Check risk score
                if (json.has("riskAnalysis")) {
                    float score = json.getAsJsonObject("riskAnalysis")
                            .get("score").getAsFloat();
                    log.info("reCAPTCHA score: {} (min={})", score, minScore);
                    return score >= minScore;
                }

                log.warn("reCAPTCHA response missing riskAnalysis");
                return false;
            }

        } catch (IOException e) {
            log.error("reCAPTCHA API call failed: {}", e.getMessage());
            // Fail open on network error so users aren't blocked by infra issues
            return true;
        }
    }
}


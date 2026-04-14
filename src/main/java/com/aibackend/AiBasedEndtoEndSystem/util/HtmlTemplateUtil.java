package com.aibackend.AiBasedEndtoEndSystem.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HtmlTemplateUtil {

    private static final String TEMPLATE_DIR = "/templates/email/";
    private static final Map<String, String> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private static final String OTP = "otp.html";
    private static final String CONTACT = "contact.html";
    private static final String SHORTLIST_APPROVED = "shortlist-approved.html";
    private static final String SHORTLIST_REJECTED = "shortlist-rejected.html";
    private static final String RECRUITER_DECISION_HIRED = "recruiter-decision-hired.html";
    private static final String RECRUITER_DECISION_REJECTED = "recruiter-decision-rejected.html";

    private HtmlTemplateUtil() {}

    public static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String otpTemplate(String otp) {
        return loadTemplate(OTP).replace("{{otp}}", escapeHtml(otp != null ? otp : ""));
    }

    public static String contactTemplate(String email, String message, String source) {
        return loadTemplate(CONTACT)
                .replace("{{email}}", email != null ? email : "")
                .replace("{{message}}", message != null ? message : "")
                .replace("{{source}}", source != null && !source.isBlank() ? source : "—");
    }

    /**
     * Candidate advanced after AI screening. {@code dashboardUrl} is the full URL (HTML-escaped for href).
     */
    public static String applicationShortlistedTemplate(
            String candidateName,
            String jobTitle,
            String companyName,
            Double score,
            String dashboardUrl) {
        String scoreSection = "";
        if (score != null) {
            String scoreVal = escapeHtml(String.format(Locale.US, "%.1f", score));
            scoreSection =
                    """
                    <p style="margin:12px 0 0;font-size:15px;color:#334155;line-height:1.6;">Your profile match score: <strong style="color:#0f172a;">%s</strong></p>
                    """
                            .formatted(scoreVal);
        }
        return loadTemplate(SHORTLIST_APPROVED)
                .replace("{{candidateName}}", escapeHtml(candidateName))
                .replace("{{jobTitle}}", escapeHtml(jobTitle))
                .replace("{{companyName}}", escapeHtml(companyName))
                .replace("{{scoreSection}}", scoreSection)
                .replace("{{dashboardUrl}}", escapeHtml(dashboardUrl != null ? dashboardUrl : ""));
    }

    public static String applicationNotShortlistedTemplate(
            String candidateName, String jobTitle, String companyName, String dashboardUrl) {
        return loadTemplate(SHORTLIST_REJECTED)
                .replace("{{candidateName}}", escapeHtml(candidateName))
                .replace("{{jobTitle}}", escapeHtml(jobTitle))
                .replace("{{companyName}}", escapeHtml(companyName))
                .replace("{{dashboardUrl}}", escapeHtml(dashboardUrl != null ? dashboardUrl : ""));
    }

    public static String applicationRecruiterHiredTemplate(
            String candidateName,
            String jobTitle,
            String companyName,
            String optionalMessage,
            String dashboardUrl) {
        String messageSection = recruiterOptionalMessageSection(optionalMessage);
        return loadTemplate(RECRUITER_DECISION_HIRED)
                .replace("{{candidateName}}", escapeHtml(candidateName))
                .replace("{{jobTitle}}", escapeHtml(jobTitle))
                .replace("{{companyName}}", escapeHtml(companyName))
                .replace("{{messageSection}}", messageSection)
                .replace("{{dashboardUrl}}", escapeHtml(dashboardUrl != null ? dashboardUrl : ""));
    }

    public static String applicationRecruiterRejectedTemplate(
            String candidateName,
            String jobTitle,
            String companyName,
            String optionalMessage,
            String dashboardUrl) {
        String messageSection = recruiterOptionalMessageSection(optionalMessage);
        return loadTemplate(RECRUITER_DECISION_REJECTED)
                .replace("{{candidateName}}", escapeHtml(candidateName))
                .replace("{{jobTitle}}", escapeHtml(jobTitle))
                .replace("{{companyName}}", escapeHtml(companyName))
                .replace("{{messageSection}}", messageSection)
                .replace("{{dashboardUrl}}", escapeHtml(dashboardUrl != null ? dashboardUrl : ""));
    }

    /**
     * Styled note from the recruiter (plain text escaped); empty string if none.
     */
    private static String recruiterOptionalMessageSection(String optionalMessage) {
        if (optionalMessage == null || optionalMessage.isBlank()) {
            return "";
        }
        return """
                <div style="margin:0 0 16px;padding:12px 14px;background:#fafafa;border-radius:10px;border:1px solid #f4f4f5;">
                <p style="margin:0 0 4px;font-size:10px;font-weight:600;color:#a1a1aa;text-transform:uppercase;letter-spacing:0.06em;">Note from employer</p>
                <p style="margin:0;font-size:13px;color:#52525b;line-height:1.5;">%s</p>
                </div>
                """
                .formatted(escapeHtml(optionalMessage.trim()).replace("\n", "<br/>"));
    }

    private static String loadTemplate(String filename) {
        return TEMPLATE_CACHE.computeIfAbsent(filename, HtmlTemplateUtil::readTemplateFile);
    }

    private static String readTemplateFile(String filename) {
        String path = TEMPLATE_DIR + filename;
        try (InputStream in = HtmlTemplateUtil.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Email template missing on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read email template: " + path, e);
        }
    }
}

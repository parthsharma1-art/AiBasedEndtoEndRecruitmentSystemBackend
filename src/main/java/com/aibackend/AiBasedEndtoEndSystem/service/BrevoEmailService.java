package com.aibackend.AiBasedEndtoEndSystem.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.controller.PublicController.ContactRequest;
import com.aibackend.AiBasedEndtoEndSystem.util.HtmlTemplateUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BrevoEmailService {

        @Value("${brevo.api.key}")
        private String apiKey;

        @Value("${brevo.sender.email}")
        private String senderEmail;

        @Value("${brevo.receiver.email}")
        private String receiverEmail;

        private void sendEmail(String toEmail, String subject, String htmlContent, String senderName) throws Exception {
                String escapedHtml = htmlContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
                String escapedSubject = subject.replace("\\", "\\\\").replace("\"", "\\\"");

                String senderJson = (senderName != null && !senderName.isBlank())
                        ? "\"sender\": {\"email\": \"%s\", \"name\": \"%s\"}"
                        : "\"sender\": {\"email\": \"%s\"}";
                String senderPart = (senderName != null && !senderName.isBlank())
                        ? String.format(senderJson, senderEmail, senderName.replace("\"", "\\\""))
                        : String.format(senderJson, senderEmail);

                String json = """
                                {
                                  %s,
                                  "to": [{"email": "%s"}],
                                  "subject": "%s",
                                  "htmlContent": "%s"
                                }
                                """.formatted(senderPart, toEmail, escapedSubject, escapedHtml);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                                .header("Content-Type", "application/json")
                                .header("api-key", apiKey)
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();

                HttpResponse<String> response = HttpClient.newHttpClient()
                                .send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Brevo status code: {}", response.statusCode());
        }

        public void sendHtmlEmail(String toEmail, String otp) throws Exception {
                String html = HtmlTemplateUtil.otpTemplate(otp);
                sendEmail(toEmail, "Your OTP Code", html, null);
        }

        public void sendContactEmail(ContactRequest request) throws Exception {
                if (ObjectUtils.isEmpty(request.getEmail()) || request.getEmail().isBlank()) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                        "Email is required");
                }
                if (ObjectUtils.isEmpty(request.getMessage()) || request.getMessage().isBlank()) {
                        throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                        "Message is required");
                }

                String safeEmail = escapeHtml(request.getEmail().trim());
                String safeMessage = escapeHtml(request.getMessage().trim());
                String safeSource = escapeHtml(request.getSource() != null ? request.getSource().trim() : "")
                                .toUpperCase();
                String htmlContent = HtmlTemplateUtil.contactTemplate(safeEmail, safeMessage, safeSource);
                String subject = "Contact form: from " + request.getEmail();

                sendEmail(receiverEmail, subject, htmlContent, "Contact Form");
        }

        private static String escapeHtml(String s) {
                if (s == null)
                        return "";
                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
}

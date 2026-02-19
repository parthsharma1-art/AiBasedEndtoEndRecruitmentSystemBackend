package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.util.HtmlTemplateUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Service
@Slf4j
public class BrevoEmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public void sendHtmlEmail(String toEmail, String otp) throws Exception {

        String html = HtmlTemplateUtil.otpTemplate(otp);

        String json = """
        {
          "sender": {"email": "%s"},
          "to": [{"email": "%s"}],
          "subject": "Your OTP Code",
          "htmlContent": "%s"
        }
        """.formatted(
                senderEmail,
                toEmail,
                html.replace("\"", "\\\"").replace("\n", "")
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Brevo status code: {}", response.statusCode());
    }


}

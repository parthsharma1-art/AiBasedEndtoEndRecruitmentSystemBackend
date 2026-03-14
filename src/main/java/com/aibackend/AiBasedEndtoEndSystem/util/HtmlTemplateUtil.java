package com.aibackend.AiBasedEndtoEndSystem.util;

public class HtmlTemplateUtil {

    public static String otpTemplate(String otp) {

        String template = """
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Verification Code</title>
        </head>

        <body style="font-family: Arial; background:#f5f6f7; padding:20px;">

            <div style="max-width:480px;margin:auto;background:white;
                        border:1px solid #e5e5e5;border-radius:12px;padding:20px;">

                <h2 style="text-align:center;">
                    AI Based End to End Recruitment System
                </h2>

                <h3 style="text-align:center;">Your Verification Code</h3>

                <p style="text-align:center;">
                    Use this code to sign in. It expires in 5 minutes.
                </p>

                <h1 style="text-align:center;
                           background:#fafafa;
                           border:1px solid #ddd;
                           padding:15px;
                           border-radius:8px;">

                    {{otp}}

                </h1>

                <p style="text-align:center;">
                    If you didn’t request this code, ignore this email.
                </p>

            </div>

        </body>
        </html>
        """;

        return template.replace("{{otp}}", otp);
    }

    /**
     * HTML template for contact form emails. Use {{email}} and {{message}} placeholders.
     */
    public static String contactTemplate(String email, String message, String source) {
        String template = """
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Contact Form Message</title>
        </head>
        <body style="font-family: Arial, sans-serif; background:#f5f6f7; padding:20px;">
            <div style="max-width:560px; margin:auto; background:white;
                        border:1px solid #e5e5e5; border-radius:12px; padding:24px;">
                <h2 style="text-align:center; color:#333;">
                    AI Based End to End Recruitment System
                </h2>
                <h3 style="color:#555; border-bottom:1px solid #eee; padding-bottom:8px;">
                    New Contact Form Submission
                </h3>
                <p style="margin:16px 0 8px; color:#666; font-size:14px;">
                    <strong>From (email):</strong>
                </p>
                <p style="margin:0 0 16px; padding:12px; background:#fafafa; border-radius:8px; border:1px solid #eee;">
                    {{email}}
                </p>
                <p style="margin:16px 0 8px; color:#666; font-size:14px;">
                    <strong>Message:</strong>
                </p>
                <p style="margin:0; padding:12px; background:#fafafa; border-radius:8px; border:1px solid #eee; white-space:pre-wrap;">
                    {{message}}
                </p>
                <p style="margin-top:20px; font-size:12px; color:#999;">
                    This email was sent via the public contact API.
                </p>
            </div>
        </body>
        </html>
        """;
        return template
                .replace("{{email}}", email == null ? "" : email)
                .replace("{{message}}", message == null ? "" : message);
    }
}

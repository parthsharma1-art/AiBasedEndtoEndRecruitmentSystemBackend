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
}

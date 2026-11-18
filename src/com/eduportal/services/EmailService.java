package com.eduportal.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class EmailService {

    public static String getEnvVar(String key, String fallback) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val.trim();

        // Try reading .env file if available
        try {
            File envFile = new File(".env");
            if (!envFile.exists()) envFile = new File("../web/.env");
            if (envFile.exists()) {
                try (var fis = new FileInputStream(envFile);
                     var br = new java.io.BufferedReader(new java.io.InputStreamReader(fis))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith(key + "=")) {
                            return line.substring(key.length() + 1).trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    public static boolean hasCustomCredentials() {
        File envFile = new File(".env");
        if (envFile.exists()) return true;
        File envFile2 = new File("../web/.env");
        return envFile2.exists();
    }

    public static String getSystemSenderEmail() {
        return getEnvVar("GMAIL_USER", "");
    }

    public static String getSystemAppPassword() {
        return getEnvVar("GMAIL_PASS", "");
    }

    public static boolean removeMailCredentials() {
        try {
            boolean removed = false;
            File envFile = new File(".env");
            if (envFile.exists()) {
                removed = envFile.delete();
            }
            File envFile2 = new File("../web/.env");
            if (envFile2.exists()) {
                removed = envFile2.delete() || removed;
            }
            return removed || !envFile.exists();
        } catch (Exception e) {
            System.err.println("[EmailService] Error removing credentials: " + e.getMessage());
            return false;
        }
    }

    public static boolean sendOtpEmail(String recipientEmail, String otpCode) {
        String senderEmail = getSystemSenderEmail();
        String appPassword  = getSystemAppPassword().replace(" ", "").trim();

        if (recipientEmail == null || recipientEmail.trim().isEmpty()) return false;
        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            System.err.println("[EmailService] Gmail credentials are not configured.");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "EduPortal System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Your EduPortal Verification Code: " + otpCode);
            message.setText("Hello,\n\nYour 6-digit email verification OTP code is: " + otpCode + 
                    "\n\nPlease enter this code in the EduPortal application to verify your account.\n\nThank you,\nEduPortal Team");

            Transport.send(message);
            System.out.println("[EmailService] Successfully sent OTP to " + recipientEmail);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + recipientEmail + ": " + e.getMessage());
            return false;
        }
    }
}

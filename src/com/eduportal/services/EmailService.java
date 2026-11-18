package com.eduportal.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailService {

    public static String getEnvVar(String key, String fallback) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val.trim();

        for (File envFile : getCredentialFiles()) {
            if (!envFile.isFile()) continue;
            try (FileInputStream fis = new FileInputStream(envFile);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(key + "=")) {
                        return line.substring(key.length() + 1).trim();
                    }
                }
            } catch (IOException e) {
                System.err.println("[EmailService] Could not read credentials from " + envFile + ": " + e.getMessage());
            }
        }
        return fallback;
    }

    public static boolean hasCustomCredentials() {
        for (File envFile : getCredentialFiles()) {
            if (envFile.isFile()) return true;
        }
        return false;
    }

    public static File getCredentialsFile() {
        String dataDirectory = System.getenv("EDUPORTAL_DATA_DIR");
        if (dataDirectory != null && !dataDirectory.trim().isEmpty()) {
            return new File(dataDirectory, ".env");
        }
        return new File(".env");
    }

    private static File[] getCredentialFiles() {
        List<File> files = new ArrayList<>();
        String dataDirectory = System.getenv("EDUPORTAL_DATA_DIR");
        if (dataDirectory != null && !dataDirectory.trim().isEmpty()) {
            files.add(new File(dataDirectory, ".env"));
            files.add(new File(new File(dataDirectory, "app"), ".env"));
        }
        files.add(new File(".env"));
        files.add(new File("../web/.env"));
        return files.toArray(new File[0]);
    }

    public static String getSystemSenderEmail() {
        return getEnvVar("GMAIL_USER", "");
    }

    public static String getSystemAppPassword() {
        return getEnvVar("GMAIL_PASS", "");
    }

    public static boolean removeMailCredentials() {
        boolean removedAll = true;
        for (File envFile : getCredentialFiles()) {
            if (envFile.exists() && !envFile.delete()) {
                removedAll = false;
            }
        }
        return removedAll;
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

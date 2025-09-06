package com.febin.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /**
     * Simulates sending a password reset email.
     * In a real application, this method would use a mail sender library (e.g., JavaMailSender)
     * to send an actual email to the user.
     *
     * @param to the recipient's email address
     * @param code the 6-digit password reset code
     */
    public void sendPasswordResetEmail(String to, String code) {
        // For development, we'll just log the email to the console.
        // This prevents the need for a configured email server during local development.
        logger.info("======================================================");
        logger.info("PASSWORD RESET EMAIL SIMULATION");
        logger.info("Recipient: {}", to);
        logger.info("Subject: Your Password Reset Code");
        logger.info("Body: Your password reset code is: {}", code);
        logger.info("This code will expire in 10 minutes.");
        logger.info("======================================================");
    }
}

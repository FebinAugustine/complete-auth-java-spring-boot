package com.febin.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromEmail,
                        @Value("${app.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    /**
     * Sends a password reset email using JavaMailSender.
     *
     * @param to the recipient's email address
     * @param code the 6-digit password reset code
     */
    public void sendPasswordResetEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Password Reset Code");
            message.setText("Your password reset code is: " + code + "\n\nThis code will expire in 10 minutes.");
            mailSender.send(message);
            logger.info("Password reset email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Error sending password reset email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an account verification email.
     *
     * @param to the recipient's email address
     * @param code the unique verification code
     */
    public void sendAccountVerificationEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Verify Your Account");
            String verificationUrl = baseUrl + "/api/auth/verify?code=" + code;
            message.setText("Please click the following link to verify your account: " + verificationUrl);
            mailSender.send(message);
            logger.info("Account verification email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Error sending account verification email to {}: {}", to, e.getMessage());
        }
    }
}

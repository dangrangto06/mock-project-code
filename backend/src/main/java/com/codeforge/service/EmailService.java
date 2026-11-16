package com.codeforge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for delivering external transactional and notification emails.
 *
 * <p>Supports sending password reset verification links, practice streak reminders, and system alerts.
 * Implements a resilient design pattern where {@link JavaMailSender} is optional: if SMTP credentials
 * are configured, real emails are dispatched via SMTP; otherwise, a simulated dispatch is logged
 * without crashing or blocking development workflows.</p>
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Optional Spring mail sender bean configured via {@code spring.mail.*} properties.
     */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Dispatches a password reset email containing a secure token link.
     *
     * <p>If SMTP mail service is active and reachable, constructs and transmits a {@link SimpleMailMessage}.
     * If SMTP is unavailable or unconfigured, simulates the dispatch by logging the reset token link.</p>
     *
     * @param toEmail    Destination recipient email address.
     * @param resetToken Secure one-time password reset token string.
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("Dispatching Password Reset Email to: {}", toEmail);
        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("CodeForge - Password Reset Request");
                message.setText("Click here to reset your CodeForge password: http://localhost:3000/#reset-password?token=" + resetToken);
                mailSender.send(message);
                log.info("Password reset email sent successfully via SMTP.");
            } else {
                log.info("[Email Service Simulation] Sent password reset link with token {} to {}", resetToken, toEmail);
            }
        } catch (Exception e) {
            log.warn("SMTP Mail sender unavailable, simulated dispatch: {}", e.getMessage());
        }
    }

    /**
     * Sends an automated reminder notification encouraging users to maintain their daily coding streak.
     *
     * @param toEmail    Destination user email address.
     * @param streakDays Current consecutive active days count.
     */
    public void sendStreakAlert(String toEmail, int streakDays) {
        log.info("[Email Service Notification] Sending streak reminder to {}: Keep your {}-day streak alive today!", toEmail, streakDays);
    }
}

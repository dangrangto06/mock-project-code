package com.codeforge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

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

    public void sendStreakAlert(String toEmail, int streakDays) {
        log.info("[Email Service Notification] Sending streak reminder to {}: Keep your {}-day streak alive today!", toEmail, streakDays);
    }
}


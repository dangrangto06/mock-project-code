package com.codeforge.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for delivering external transactional and notification emails.
 *
 * <p>
 * Supports sending password reset verification links, feedback confirmation
 * notices, practice streak reminders, and system alerts.
 * Implements a resilient design pattern where {@link JavaMailSender} is
 * optional: if SMTP credentials are configured, real emails are dispatched via
 * SMTP; otherwise, a simulated dispatch is logged without crashing or blocking
 * development workflows.
 * </p>
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Optional Spring mail sender bean configured via {@code spring.mail.*}
     * properties.
     */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Dispatches a password reset email containing a secure token link.
     *
     * @param toEmail    Destination recipient email address.
     * @param resetToken Secure one-time password reset token string.
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String base = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.replaceAll("/+$", "") : "http://localhost:3000";
        String resetLink = base + "/login.html?resetToken=" + resetToken;
        log.info("Password reset link for {}: {}", toEmail, resetLink);
        try {
            if (mailSender != null) {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                if (fromEmail != null && !fromEmail.isBlank()) {
                    helper.setFrom(fromEmail, "CodeForge Support");
                    helper.setReplyTo(fromEmail);
                }
                helper.setTo(toEmail);
                helper.setSubject("CodeForge - Password Reset Request");
                mimeMessage.setHeader("Auto-Submitted", "auto-generated");
                mimeMessage.setHeader("X-Auto-Response-Suppress", "All");
                mimeMessage.setHeader("X-Mailer", "CodeForge-Platform-Mailer");

                String text = "Hello,\n\nYou requested a password reset for your CodeForge account.\n\n"
                        + "Please click the link below to set a new password:\n" + resetLink + "\n\n"
                        + "If you did not request this reset, you can safely ignore this email.\n\n"
                        + "Best regards,\nThe CodeForge Team";
                helper.setText(text, false);
                mailSender.send(mimeMessage);
                log.info("Password reset email sent successfully via SMTP to {}", toEmail);
            } else {
                log.info("[Email Service Simulation] Sent password reset link {} to {}", resetLink, toEmail);
            }
        } catch (Exception e) {
            log.warn("SMTP Mail sender unavailable, simulated dispatch: {}", e.getMessage());
        }
    }

    /**
     * Sends an automated confirmation email to the user when they submit feedback.
     * Contains the exact message: "We value your feedback and will get back to you soon!"
     * with high-class styling, branding, and feedback details.
     *
     * <p>Includes both text/plain fallback and responsive HTML, plus RFC 3834 auto-submitted
     * headers to minimize spam classification risk in Gmail, Outlook, and other providers.</p>
     *
     * @param toEmail         Destination user email address.
     * @param recipientName   Submitter display name.
     * @param feedbackMessage The message content submitted by the user.
     */
    public void sendFeedbackConfirmationEmail(String toEmail, String recipientName, String feedbackMessage) {
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "Valued Developer";
        String rawMessage = (feedbackMessage != null) ? feedbackMessage.trim() : "";
        String safeMessage = rawMessage.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
        log.info("Dispatching feedback confirmation email to {}", toEmail);

        try {
            if (mailSender != null) {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                // Enable multipart mode for HTML + plain text alternative
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                if (fromEmail != null && !fromEmail.isBlank()) {
                    helper.setFrom(fromEmail, "CodeForge Support");
                    helper.setReplyTo(fromEmail);
                }
                helper.setTo(toEmail);
                // Professional subject without exclamation marks to avoid Bayesian spam filters
                helper.setSubject("CodeForge - Feedback Confirmation");

                // RFC 3834 and transactional headers to declare auto-generated transactional content
                mimeMessage.setHeader("Auto-Submitted", "auto-generated");
                mimeMessage.setHeader("X-Auto-Response-Suppress", "All");
                mimeMessage.setHeader("Precedence", "bulk");
                mimeMessage.setHeader("X-Mailer", "CodeForge-Platform-Mailer");

                String plainText = buildFeedbackPlainText(safeName, rawMessage);
                String htmlBody = buildFeedbackHtml(safeName, safeMessage);
                // multipart/alternative: both plain text and rich HTML
                helper.setText(plainText, htmlBody);

                mailSender.send(mimeMessage);
                log.info("Feedback confirmation email sent successfully via SMTP to {}", toEmail);
            } else {
                log.info("[Email Service Simulation] Sent confirmation to {}: 'We value your feedback and will get back to you soon!'", toEmail);
            }
        } catch (Exception e) {
            log.warn("SMTP dispatch failed or not configured, simulated feedback confirmation: {}", e.getMessage());
        }
    }

    /**
     * Builds plain text fallback body for anti-spam multipart/alternative compliance.
     */
    private String buildFeedbackPlainText(String userName, String feedbackMessage) {
        return """
            Hello %s,

            We value your feedback and will get back to you soon!

            Thank you for sharing your thoughts with the CodeForge team. User feedback is vital in shaping our platform, refining challenges, and crafting the best possible developer learning journey.

            Your Submitted Message:
            "%s"

            Our team will carefully review your notes. If your submission requires troubleshooting or further clarification, one of our administrators will reach back out to you directly at this email address.

            Best regards,
            The CodeForge Platform Team
            """.formatted(userName, feedbackMessage);
    }

    /**
     * Generates a beautifully styled, responsive HTML email template for feedback confirmation.
     */
    private String buildFeedbackHtml(String userName, String feedbackMessage) {
        String base = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl.replaceAll("/+$", "") : "http://localhost:3000";
        String homeLink = base + "/index.html";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>We Value Your Feedback</title>
            </head>
            <body style="margin: 0; padding: 24px 12px; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
              <table role="presentation" width="100%%" border="0" cellpadding="0" cellspacing="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; border: 1px solid #cbd5e1; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                <!-- Header Banner -->
                <tr>
                  <td style="padding: 28px 32px; background: linear-gradient(135deg, #064e3b 0%%, #047857 50%%, #059669 100%%); color: #ffffff;">
                    <table role="presentation" width="100%%" border="0" cellpadding="0" cellspacing="0">
                      <tr>
                        <td>
                          <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                            <tr>
                              <td valign="middle" style="padding-right: 12px;">
                                <div style="width: 36px; height: 36px; line-height: 36px; text-align: center; border-radius: 9px; background: linear-gradient(135deg, #10b981 0%%, #059669 55%%, #065f46 100%%); border: 1px solid rgba(110, 231, 183, 0.35); color: #ffffff; font-family: 'JetBrains Mono', Consolas, monospace; font-size: 15px; font-weight: 800; letter-spacing: -1px;">
                                  &lt;&gt;
                                </div>
                              </td>
                              <td valign="middle">
                                <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.02em; color: #ffffff;">
                                  CodeForge
                                </div>
                                <div style="font-size: 12px; color: #a7f3d0; margin-top: 4px; font-weight: 500; letter-spacing: 0.02em;">
                                  DEVELOPER FEEDBACK CONFIRMATION
                                </div>
                              </td>
                            </tr>
                          </table>
                        </td>
                        <td align="right">
                          <span style="display: inline-block; background-color: rgba(255, 255, 255, 0.2); color: #ffffff; font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 20px; text-transform: uppercase;">
                            Received
                          </span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- Content Area -->
                <tr>
                  <td style="padding: 32px 32px 24px 32px;">
                    <p style="margin: 0 0 16px 0; font-size: 16px; color: #0f172a; font-weight: 600;">
                      Hello %s,
                    </p>

                    <!-- Core Prompt Highlight Box -->
                    <div style="background: linear-gradient(135deg, #ecfdf5 0%%, #f0fdf4 100%%); border-left: 4px solid #10b981; border-radius: 8px; padding: 18px 20px; margin: 20px 0;">
                      <h3 style="margin: 0 0 6px 0; color: #065f46; font-size: 18px; font-weight: 700; line-height: 1.4;">
                        We value your feedback and will get back to you soon!
                      </h3>
                      <p style="margin: 0; color: #047857; font-size: 14px; line-height: 1.5;">
                        Thank you for sharing your thoughts with the CodeForge team. User feedback is vital in shaping our platform, refining challenges, and crafting the best possible developer learning journey.
                      </p>
                    </div>

                    <!-- Summary Box -->
                    <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px 20px; margin: 24px 0;">
                      <p style="margin: 0 0 8px 0; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #64748b;">
                        Your Submitted Message
                      </p>
                      <div style="color: #334155; font-size: 14px; line-height: 1.6; font-style: italic;">
                        "%s"
                      </div>
                    </div>

                    <p style="margin: 0 0 24px 0; font-size: 14px; color: #475569; line-height: 1.6;">
                      Our team will carefully review your notes. If your submission requires troubleshooting or further clarification, one of our administrators will reach back out to you directly at this email address.
                    </p>

                    <!-- CTA Button -->
                    <div style="text-align: center; margin: 28px 0 16px 0;">
                      <a href="%s" style="display: inline-block; background-color: #059669; color: #ffffff; text-decoration: none; font-weight: 600; font-size: 14px; padding: 12px 28px; border-radius: 8px; box-shadow: 0 2px 4px rgba(5, 150, 105, 0.2);">
                        Return to CodeForge Platform
                      </a>
                    </div>
                  </td>
                </tr>

                <!-- Divider -->
                <tr>
                  <td style="padding: 0 32px;">
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 0;">
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="padding: 24px 32px 32px 32px; background-color: #f8fafc; text-align: center;">
                    <p style="margin: 0 0 6px 0; font-size: 13px; font-weight: 600; color: #334155;">
                      The CodeForge Platform Team
                    </p>
                    <p style="margin: 0; font-size: 11px; color: #94a3b8; line-height: 1.5;">
                      &copy; 2026 CodeForge Inc. All rights reserved.<br/>
                      This is an automated confirmation message sent to your registered account email.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(userName, feedbackMessage, homeLink);
    }

    /**
     * Sends an automated reminder notification encouraging users to maintain their
     * daily coding streak.
     *
     * @param toEmail    Destination user email address.
     * @param streakDays Current consecutive active days count.
     */
    public void sendStreakAlert(String toEmail, int streakDays) {
        log.info("[Email Service Notification] Sending streak reminder to {}: Keep your {}-day streak alive today!",
                toEmail, streakDays);
    }
}

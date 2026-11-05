package com.olp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending real email notifications (OTPs, password reset links).
 *
 * Developer B: Real Email Delivery System.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an OTP code to the user's real email address.
     *
     * @param toEmail       recipient's email address
     * @param name          recipient's full name (shown in greeting)
     * @param otpCode       6-digit OTP code
     * @param purpose       e.g. "Password Reset" or "Login Authentication"
     * @param expiryMinutes number of minutes before OTP expires
     */
    public void sendOtpEmail(String toEmail, String name, String otpCode, String purpose, int expiryMinutes) {
        if (senderEmail == null || senderEmail.isBlank()) {
            log.warn("[EmailService] No SMTP username configured in application.properties. Email not dispatched to SMTP network.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Focotech");
            helper.setTo(toEmail);
            helper.setSubject("Focotech Verification Code: " + otpCode);

            String htmlContent = buildOtpHtml(toEmail, name, otpCode, purpose, expiryMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Successfully sent OTP email to: {}", toEmail);

        } catch (org.springframework.mail.MailSendException e) {
            log.error("[EmailService] SMTP delivery rejection for {}: {}", toEmail, e.getMessage());
            throw new IllegalArgumentException("Email does not exist or was rejected by the mail server. Please check your email address.");
        } catch (MessagingException e) {
            log.error("[EmailService] Messaging error sending email to {}: {}", toEmail, e.getMessage());
            throw new IllegalArgumentException("Failed to format or send email to " + toEmail + ". Please check your address.");
        } catch (Exception e) {
            log.error("[EmailService] Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new IllegalArgumentException("Email delivery failed: " + e.getMessage());
        }
    }

    private String buildOtpHtml(String email, String name, String otpCode, String purpose, int expiryMinutes) {
        String displayName = (name != null && !name.isBlank()) ? name.trim() : email;
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verification Code</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f7f4f1; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f4f1; padding: 48px 16px;">
                    <tr>
                      <td align="center">
                        <!-- Main Card -->
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 520px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e8e2dc; box-shadow: 0 4px 16px rgba(30, 27, 25, 0.04);">
                          
                          <!-- Top Brand Accent Line (Focotech Signature Coral) -->
                          <tr>
                            <td style="background-color: #ff5a36; height: 4px; border-top-left-radius: 11px; border-top-right-radius: 11px;"></td>
                          </tr>

                          <!-- Header with Wordmark -->
                          <tr>
                            <td style="padding: 36px 40px 12px 40px; text-align: left;">
                              <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e1b19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                FOCO<span style="color: #ff5a36;">TECH</span>
                              </div>
                            </td>
                          </tr>

                          <!-- Body Content -->
                          <tr>
                            <td style="padding: 12px 40px 32px 40px;">
                              <h1 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 700; color: #1e1b19; letter-spacing: -0.3px;">
                                Verification Code
                              </h1>
                              
                              <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 24px; color: #443e3a;">
                                Hello, <strong style="color: #1e1b19;">%s</strong>
                              </p>
                              
                              <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 24px; color: #443e3a;">
                                Please use the 6-digit verification code below to complete your %s on Focotech (<span style="color: #1e1b19; font-weight: 600;">%s</span>):
                              </p>

                              <!-- Focotech Minimal OTP Container -->
                              <div style="background-color: #faf7f4; border: 1px solid #eae3dc; border-radius: 8px; padding: 24px 16px; text-align: center; margin: 24px 0 16px 0;">
                                <div style="font-family: 'SFMono-Regular', Consolas, Menlo, Monaco, monospace; font-size: 38px; font-weight: 700; letter-spacing: 10px; color: #1e1b19; padding-left: 10px;">
                                  %s
                                </div>
                                <div style="margin-top: 10px; font-size: 12px; font-weight: 500; color: #786f68;">
                                  Valid for %d minutes &bull; One-time use only
                                </div>
                              </div>

                              <!-- Clean Security Notice -->
                              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #faf7f4; border-left: 3px solid #ff5a36; border-radius: 4px; padding: 12px 14px; margin: 24px 0 20px 0;">
                                <tr>
                                  <td>
                                    <p style="margin: 0; font-size: 12px; line-height: 18px; color: #5a524c;">
                                      <strong style="color: #1e1b19;">Security Notice:</strong> Never share this code with anyone. Focotech staff will never ask for your verification code.
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin: 20px 0 0 0; font-size: 13px; line-height: 20px; color: #8a817a;">
                                If you did not make this request, you can safely ignore this email.
                              </p>
                            </td>
                          </tr>

                          <!-- Divider -->
                          <tr>
                            <td style="padding: 0 40px;">
                              <div style="border-top: 1px solid #eee7e2;"></div>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding: 24px 40px; background-color: #faf7f4; text-align: left; border-bottom-left-radius: 11px; border-bottom-right-radius: 11px;">
                              <p style="margin: 0 0 4px 0; font-size: 12px; color: #5a524c; font-weight: 600;">
                                Focotech &bull; Master In-Demand Tech Skills
                              </p>
                              <p style="margin: 0; font-size: 11px; color: #8a817a; line-height: 16px;">
                                &copy; 2026 Focotech. All rights reserved. &bull; Automated message, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                        <!-- End Main Card -->
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, purpose, email, otpCode, expiryMinutes);
    }

}

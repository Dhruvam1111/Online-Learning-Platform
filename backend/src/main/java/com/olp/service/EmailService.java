package com.olp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending real email notifications (OTPs, payment receipts, refund credit notes).
 * Owned by Shubh (Notification & Receipts Engine).
 */
@Service
public class EmailService {
    @org.springframework.beans.factory.annotation.Autowired(required=false) private PaymentPolicyService policies;
    private BigDecimal taxDivisor(String orderId) { return BigDecimal.ONE.add(policies==null?new BigDecimal("0.18"):policies.value(policies.forOrder(orderId),"taxRate")); }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an OTP code to the user's real email address.
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

    /**
     * Sends an official payment confirmation & GST receipt email to the student upon successful enrollment.
     */
    public void sendPaymentSuccessEmail(String toEmail, String name, String courseTitle,
                                        String orderId, String transactionId,
                                        String invoiceNumber, BigDecimal amount,
                                        String invoiceUrl) {
        sendPaymentSuccessEmail(toEmail, name, courseTitle, orderId, transactionId, invoiceNumber, amount, invoiceUrl, null);
    }

    /**
     * Overload that attaches the actual receipt HTML file directly to the email for immediate offline download.
     */
    public void sendPaymentSuccessEmail(String toEmail, String name, String courseTitle,
                                        String orderId, String transactionId,
                                        String invoiceNumber, BigDecimal amount,
                                        String invoiceUrl, String invoiceHtml) {
        String displayName = (name != null && !name.isBlank()) ? name.trim() : toEmail;
        log.info("[EmailService] Dispatched Payment Receipt Email to {} for Course '{}' (Txn: {}, Invoice: {}, ₹{})",
                toEmail, courseTitle, transactionId, invoiceNumber, amount);

        if (senderEmail == null || senderEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Focotech");
            helper.setTo(toEmail);
            helper.setSubject("Payment Confirmed: Enrollment in " + courseTitle);

            String html = buildPaymentSuccessHtml(displayName, toEmail, courseTitle, invoiceNumber, transactionId, orderId, amount, invoiceUrl);
            helper.setText(html, true);

            // Always attach the official tax invoice receipt file (.html)
            final String filename = "Tax-Invoice-" + (invoiceNumber != null && !invoiceNumber.isBlank() ? invoiceNumber : orderId) + ".html";
            String receiptHtmlToAttach = (invoiceHtml != null && !invoiceHtml.isBlank())
                    ? invoiceHtml
                    : buildStandaloneReceiptHtml(displayName, toEmail, courseTitle, invoiceNumber, transactionId, orderId, amount);

            helper.addAttachment(
                    filename,
                    new ByteArrayResource(receiptHtmlToAttach.getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    },
                    "text/html; charset=UTF-8"
            );
            log.info("[EmailService] Attached receipt file '{}' directly to email for {}", filename, toEmail);

            mailSender.send(message);
            log.info("[EmailService] Real SMTP receipt dispatched to: {}", toEmail);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send real email via SMTP ({}), continuing transaction.", e.getMessage());
        }
    }

    /**
     * Sends an email notification to the student when a payment attempt fails or is cancelled.
     */
    public void sendPaymentFailedEmail(String toEmail, String name, String courseTitle,
                                       String orderId, String failureReason) {
        String displayName = (name != null && !name.isBlank()) ? name.trim() : toEmail;
        String reason = (failureReason != null && !failureReason.isBlank()) ? failureReason : "Payment authorization was not completed.";
        log.info("[EmailService] Dispatched Payment Failure Notification to {} for Order '{}' (Course: '{}')",
                toEmail, orderId, courseTitle);

        if (senderEmail == null || senderEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Focotech");
            helper.setTo(toEmail);
            helper.setSubject("Payment Alert: Course Enrollment Not Completed");

            String html = buildPaymentFailedHtml(displayName, courseTitle, orderId, reason);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EmailService] Real SMTP failure notification dispatched to: {}", toEmail);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send failure email via SMTP ({}), continuing.", e.getMessage());
        }
    }

    /**
     * Sends an official refund confirmation & reversal notice to the student upon successful refund (FR-PG05, FR-PG06).
     */
    public void sendRefundConfirmationEmail(String toEmail, String name, String courseTitle,
                                           String orderId, String refundId, BigDecimal refundAmount,
                                           String reason) {
        String displayName = (name != null && !name.isBlank()) ? name.trim() : toEmail;
        String refundReason = (reason != null && !reason.isBlank()) ? reason : "Student requested 100% money-back refund.";
        log.info("[EmailService] Dispatched Refund Confirmation Email to {} for Course '{}' (Refund: {}, Order: {}, ₹{})",
                toEmail, courseTitle, refundId, orderId, refundAmount);

        if (senderEmail == null || senderEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Focotech");
            helper.setTo(toEmail);
            helper.setSubject("Refund Confirmed: " + courseTitle);

            String html = buildRefundConfirmationHtml(displayName, toEmail, courseTitle, orderId, refundId, refundAmount, refundReason);
            helper.setText(html, true);

            // Attach official refund credit note receipt directly to email for instant download
            final String refundFilename = "Refund-Receipt-" + refundId + ".html";
            String refundReceiptHtml = buildStandaloneRefundReceiptHtml(displayName, toEmail, courseTitle, orderId, refundId, refundAmount, refundReason);
            helper.addAttachment(
                    refundFilename,
                    new ByteArrayResource(refundReceiptHtml.getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                            return refundFilename;
                        }
                    },
                    "text/html; charset=UTF-8"
            );
            log.info("[EmailService] Attached refund credit note '{}' directly to email for {}", refundFilename, toEmail);

            mailSender.send(message);
            log.info("[EmailService] Real SMTP refund confirmation email dispatched to: {}", toEmail);
        } catch (Exception e) {
            log.warn("[EmailService] Failed to send refund confirmation email via SMTP ({}), continuing transaction.", e.getMessage());
        }
    }

    private String buildPaymentSuccessHtml(String name, String toEmail, String courseTitle, String invoiceNumber,
                                          String transactionId, String orderId, BigDecimal amount,
                                          String invoiceUrl) {
        BigDecimal safeAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? amount : new BigDecimal("499.00");
        BigDecimal baseAmount = safeAmount.divide(taxDivisor(orderId), 2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = safeAmount.subtract(baseAmount);
        BigDecimal cgst = gstAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = gstAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

        String invNum = (invoiceNumber != null && !invoiceNumber.isBlank()) ? invoiceNumber : "INV-202611-" + (orderId != null && orderId.length() >= 6 ? orderId.substring(orderId.length() - 6) : "000000");
        String txnId = (transactionId != null && !transactionId.isBlank()) ? transactionId : "TXN-" + System.currentTimeMillis();
        String ordId = (orderId != null && !orderId.isBlank()) ? orderId : "OLP-ORD";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Payment Receipt</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f7f4f1; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f4f1; padding: 36px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 580px; background-color: #ffffff; border-radius: 14px; border: 1px solid #e8e2dc; box-shadow: 0 4px 20px rgba(30, 27, 25, 0.05); overflow: hidden;">
                          
                          <!-- Top Brand Accent Line -->
                          <tr>
                            <td style="background-color: #ff5a36; height: 4px;"></td>
                          </tr>

                          <!-- Header with Wordmark -->
                          <tr>
                            <td style="padding: 28px 32px 8px 32px; text-align: left;">
                              <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e1b19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                FOCO<span style="color: #ff5a36;">TECH</span>
                              </div>
                            </td>
                          </tr>

                          <!-- Body Content -->
                          <tr>
                            <td style="padding: 12px 32px 28px 32px;">
                              <h1 style="margin: 0 0 10px 0; font-size: 22px; font-weight: 800; color: #1e1b19; letter-spacing: -0.3px;">
                                Payment Confirmed & Enrolled! 🎉
                              </h1>
                              
                              <p style="margin: 0 0 14px 0; font-size: 14px; line-height: 22px; color: #443e3a;">
                                Hello, <strong style="color: #1e1b19;">%s</strong>
                              </p>
                              
                              <p style="margin: 0 0 18px 0; font-size: 14px; line-height: 22px; color: #443e3a;">
                                Your payment for <strong style="color: #1e1b19;">"%s"</strong> was successful. Lifetime course access and assignment materials are now active on your dashboard.
                              </p>

                              <!-- Embedded Full GST Tax Invoice Receipt -->
                              <div style="background-color: #ffffff; border: 1px solid #e5ded7; border-radius: 10px; margin: 20px 0 18px 0; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.03);">
                                
                                <!-- Receipt Card Header -->
                                <div style="background-color: #faf6f2; border-bottom: 2px solid #ff5a36; padding: 16px 20px;">
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                    <tr>
                                      <td style="vertical-align: top;">
                                        <div style="font-size: 16px; font-weight: 800; color: #1e1b19;">
                                          FOCO<span style="color: #ff5a36;">TECH</span>
                                        </div>
                                        <div style="font-size: 11px; color: #786f68; margin-top: 2px; line-height: 15px;">
                                          Online Learning Technologies Pvt. Ltd.<br>
                                          GSTIN: 24AAACF1234F1Z5 &bull; HSN: 999293
                                        </div>
                                      </td>
                                      <td style="text-align: right; vertical-align: top;">
                                        <span style="display: inline-block; background-color: #fff0eb; color: #ff5a36; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 12px; text-transform: uppercase;">OFFICIAL TAX INVOICE</span>
                                        <div style="font-family: 'SFMono-Regular', Consolas, monospace; font-size: 12px; font-weight: 700; color: #1e1b19; margin-top: 4px;">%s</div>
                                        <div style="font-size: 11px; color: #786f68;">Issued: %s</div>
                                      </td>
                                    </tr>
                                  </table>
                                </div>

                                <!-- Receipt Billed Details -->
                                <div style="padding: 16px 20px;">
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 12px; line-height: 18px; margin-bottom: 14px;">
                                    <tr>
                                      <td style="width: 50%%; vertical-align: top;">
                                        <div style="font-size: 10px; font-weight: 700; color: #786f68; text-transform: uppercase; margin-bottom: 2px;">BILLED TO (STUDENT)</div>
                                        <strong style="color: #1e1b19; font-size: 13px;">%s</strong><br>
                                        <span style="color: #6d635c;">%s</span>
                                      </td>
                                      <td style="width: 50%%; vertical-align: top; text-align: right;">
                                        <div style="font-size: 10px; font-weight: 700; color: #786f68; text-transform: uppercase; margin-bottom: 2px;">PAYMENT DETAILS</div>
                                        Order Ref: <code style="font-family: monospace; color: #1e1b19; font-weight: 600;">%s</code><br>
                                        Txn ID: <code style="font-family: monospace; color: #ff5a36; font-weight: 700;">%s</code><br>
                                        Status: <strong style="color: #047857;">Paid & Confirmed</strong>
                                      </td>
                                    </tr>
                                  </table>

                                  <!-- Item Table -->
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse: collapse; font-size: 12px; margin-bottom: 14px;">
                                    <thead>
                                      <tr style="background-color: #faf6f2; border-top: 1px solid #eae3dc; border-bottom: 1px solid #eae3dc;">
                                        <th style="text-align: left; padding: 8px 10px; color: #6d635c; font-weight: 700;">Course Description</th>
                                        <th style="text-align: center; padding: 8px 10px; color: #6d635c; font-weight: 700;">HSN</th>
                                        <th style="text-align: right; padding: 8px 10px; color: #6d635c; font-weight: 700;">Amount (INR)</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      <tr>
                                        <td style="padding: 10px; border-bottom: 1px solid #f0dfd3; color: #1e1b19;">
                                          <strong style="font-size: 13px;">%s</strong><br>
                                          <span style="font-size: 11px; color: #786f68;">Full Lifetime Course Access with Verified Certificate</span>
                                        </td>
                                        <td style="text-align: center; padding: 10px; border-bottom: 1px solid #f0dfd3; color: #6d635c;">999293</td>
                                        <td style="text-align: right; padding: 10px; border-bottom: 1px solid #f0dfd3; font-weight: 700; color: #1e1b19;">₹%s</td>
                                      </tr>
                                    </tbody>
                                  </table>

                                  <!-- Financial Breakdown -->
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 12px; line-height: 20px;">
                                    <tr>
                                      <td style="width: 45%%;"></td>
                                      <td style="width: 55%%;">
                                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                          <tr>
                                            <td style="color: #6d635c; padding: 2px 0;">Taxable Base Value:</td>
                                            <td style="text-align: right; color: #1e1b19; padding: 2px 0;">₹%s</td>
                                          </tr>
                                          <tr>
                                            <td style="color: #6d635c; padding: 2px 0;">CGST:</td>
                                            <td style="text-align: right; color: #1e1b19; padding: 2px 0;">₹%s</td>
                                          </tr>
                                          <tr>
                                            <td style="color: #6d635c; padding: 2px 0;">SGST:</td>
                                            <td style="text-align: right; color: #1e1b19; padding: 2px 0;">₹%s</td>
                                          </tr>
                                          <tr style="border-top: 2px solid #1e1b19;">
                                            <td style="font-size: 13px; font-weight: 800; color: #1e1b19; padding: 6px 0 0 0;">Total Paid:</td>
                                            <td style="text-align: right; font-size: 16px; font-weight: 800; color: #1e1b19; padding: 6px 0 0 0;">₹%s</td>
                                          </tr>
                                        </table>
                                      </td>
                                    </tr>
                                  </table>
                                </div>

                                <div style="background-color: #faf6f2; padding: 10px 20px; border-top: 1px dashed #eae3dc; text-align: center; font-size: 11px; color: #786f68;">
                                  ✓ Official Computer-Generated GST Tax Invoice &bull; Authorized by Focotech
                                </div>
                              </div>

                              <!-- Instant Receipt Action Notice -->
                              <div style="background-color: #fbf7f4; border-left: 3px solid #ff5a36; border-radius: 6px; padding: 12px 14px; margin: 18px 0 16px 0;">
                                <div style="font-size: 12px; line-height: 18px; color: #5a524c;">
                                  <strong style="color: #1e1b19;">📥 Download or Print Receipt:</strong><br>
                                  1. An official receipt file (<strong>Tax-Invoice-%s.html</strong>) is <strong>attached directly to this email</strong>.<br>
                                  2. You can also print or save as PDF right now by pressing <strong>Ctrl+P (or ⌘P)</strong> in your email client.
                                </div>
                              </div>

                              <p style="font-size: 12px; color: #5a524c;">
                                To retrieve the receipt later, sign in and open Transactions on your student dashboard.
                              </p>

                              <p style="margin: 18px 0 0 0; font-size: 13px; line-height: 20px; color: #8a817a;">
                                Thank you for learning with Focotech. Happy learning!
                              </p>
                            </td>
                          </tr>

                          <!-- Divider -->
                          <tr>
                            <td style="padding: 0 32px;">
                              <div style="border-top: 1px solid #eee7e2;"></div>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding: 20px 32px; background-color: #faf7f4; text-align: left;">
                              <p style="margin: 0 0 4px 0; font-size: 12px; color: #5a524c; font-weight: 600;">
                                Focotech &bull; Master In-Demand Tech Skills
                              </p>
                              <p style="margin: 0; font-size: 11px; color: #8a817a; line-height: 16px;">
                                &copy; 2026 Focotech. All rights reserved. &bull; Automated message, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        name, courseTitle, invNum, dateStr,
                        name, toEmail, ordId, txnId,
                        courseTitle, safeAmount.toPlainString(),
                        baseAmount.toPlainString(), cgst.toPlainString(), sgst.toPlainString(), safeAmount.toPlainString(),
                        invNum
                );
    }

    private String buildRefundConfirmationHtml(String name, String toEmail, String courseTitle, String orderId,
                                               String refundId, BigDecimal refundAmount,
                                               String refundReason) {
        BigDecimal safeRefund = (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) ? refundAmount : new BigDecimal("499.00");
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String ordId = (orderId != null && !orderId.isBlank()) ? orderId : "OLP-ORD";
        String rfdId = (refundId != null && !refundId.isBlank()) ? refundId : "RFD-" + System.currentTimeMillis();

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Refund Confirmation</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f7f4f1; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f4f1; padding: 36px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 580px; background-color: #ffffff; border-radius: 14px; border: 1px solid #e8e2dc; box-shadow: 0 4px 20px rgba(30, 27, 25, 0.05); overflow: hidden;">
                          
                          <!-- Top Brand Accent Line (Focotech Amber Reversal) -->
                          <tr>
                            <td style="background-color: #d97706; height: 4px;"></td>
                          </tr>

                          <!-- Header with Wordmark -->
                          <tr>
                            <td style="padding: 28px 32px 8px 32px; text-align: left;">
                              <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e1b19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                FOCO<span style="color: #ff5a36;">TECH</span>
                              </div>
                            </td>
                          </tr>

                          <!-- Body Content -->
                          <tr>
                            <td style="padding: 12px 32px 28px 32px;">
                              <h1 style="margin: 0 0 10px 0; font-size: 22px; font-weight: 800; color: #1e1b19; letter-spacing: -0.3px;">
                                Refund Processed & Confirmed ↩️
                              </h1>
                              
                              <p style="margin: 0 0 14px 0; font-size: 14px; line-height: 22px; color: #443e3a;">
                                Hello, <strong style="color: #1e1b19;">%s</strong>
                              </p>
                              
                              <p style="margin: 0 0 18px 0; font-size: 14px; line-height: 22px; color: #443e3a;">
                                Your refund request for <strong style="color: #1e1b19;">"%s"</strong> has been processed successfully under our 100%% Money-Back Guarantee policy.
                              </p>

                              <!-- Embedded Full Official Refund Credit Note Card -->
                              <div style="background-color: #ffffff; border: 1px solid #e5ded7; border-radius: 10px; margin: 20px 0 18px 0; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.03);">
                                
                                <div style="background-color: #fffbf5; border-bottom: 2px solid #d97706; padding: 16px 20px;">
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                                    <tr>
                                      <td style="vertical-align: top;">
                                        <div style="font-size: 16px; font-weight: 800; color: #1e1b19;">
                                          FOCO<span style="color: #ff5a36;">TECH</span>
                                        </div>
                                        <div style="font-size: 11px; color: #786f68; margin-top: 2px; line-height: 15px;">
                                          Online Learning Technologies Pvt. Ltd.<br>
                                          GSTIN: 24AAACF1234F1Z5
                                        </div>
                                      </td>
                                      <td style="text-align: right; vertical-align: top;">
                                        <span style="display: inline-block; background-color: #fef3c7; color: #b45309; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 12px; text-transform: uppercase;">OFFICIAL CREDIT NOTE</span>
                                        <div style="font-family: 'SFMono-Regular', Consolas, monospace; font-size: 12px; font-weight: 700; color: #1e1b19; margin-top: 4px;">CN-%s</div>
                                        <div style="font-size: 11px; color: #786f68;">Issued: %s</div>
                                      </td>
                                    </tr>
                                  </table>
                                </div>

                                <div style="padding: 16px 20px;">
                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 12px; line-height: 18px; margin-bottom: 14px;">
                                    <tr>
                                      <td style="width: 50%%; vertical-align: top;">
                                        <div style="font-size: 10px; font-weight: 700; color: #786f68; text-transform: uppercase; margin-bottom: 2px;">STUDENT RECIPIENT</div>
                                        <strong style="color: #1e1b19; font-size: 13px;">%s</strong><br>
                                        <span style="color: #6d635c;">%s</span>
                                      </td>
                                      <td style="width: 50%%; vertical-align: top; text-align: right;">
                                        <div style="font-size: 10px; font-weight: 700; color: #786f68; text-transform: uppercase; margin-bottom: 2px;">REVERSAL REFERENCE</div>
                                        Original Order: <code style="font-family: monospace; color: #1e1b19; font-weight: 600;">%s</code><br>
                                        Refund Ref: <code style="font-family: monospace; color: #d97706; font-weight: 700;">%s</code><br>
                                        Status: <strong style="color: #d97706;">Refund Completed (Reversed)</strong>
                                      </td>
                                    </tr>
                                  </table>

                                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse: collapse; font-size: 12px; margin-bottom: 14px;">
                                    <thead>
                                      <tr style="background-color: #fffbf5; border-top: 1px solid #eae3dc; border-bottom: 1px solid #eae3dc;">
                                        <th style="text-align: left; padding: 8px 10px; color: #6d635c; font-weight: 700;">Course Description</th>
                                        <th style="text-align: center; padding: 8px 10px; color: #6d635c; font-weight: 700;">Policy</th>
                                        <th style="text-align: right; padding: 8px 10px; color: #6d635c; font-weight: 700;">Refund Amount (INR)</th>
                                      </tr>
                                    </thead>
                                    <tbody>
                                      <tr>
                                        <td style="padding: 10px; border-bottom: 1px solid #f0dfd3; color: #1e1b19;">
                                          <strong style="font-size: 13px;">%s</strong><br>
                                          <span style="font-size: 11px; color: #786f68;">Course Access Revoked under 100%% Money-Back Guarantee</span>
                                        </td>
                                        <td style="text-align: center; padding: 10px; border-bottom: 1px solid #f0dfd3; color: #047857; font-weight: 700;">100%% Guarantee</td>
                                        <td style="text-align: right; padding: 10px; border-bottom: 1px solid #f0dfd3; font-weight: 800; color: #047857; font-size: 15px;">₹%s</td>
                                      </tr>
                                    </tbody>
                                  </table>

                                  <div style="background-color: #faf7f4; border-radius: 6px; padding: 10px 14px; font-size: 12px; color: #5a524c; line-height: 18px;">
                                    <strong>Recorded Reason:</strong> %s
                                  </div>
                                </div>

                                <div style="background-color: #fffbf5; padding: 10px 20px; border-top: 1px dashed #eae3dc; text-align: center; font-size: 11px; color: #786f68;">
                                  ✓ Official Computer-Generated Credit Note &bull; Authorized by Focotech Refund Engine
                                </div>
                              </div>

                              <!-- Credit Timeline Notice -->
                              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #faf7f4; border-left: 3px solid #d97706; border-radius: 4px; padding: 12px 14px; margin: 18px 0 16px 0;">
                                <tr>
                                  <td>
                                    <p style="margin: 0; font-size: 12px; line-height: 18px; color: #5a524c;">
                                      <strong style="color: #1e1b19;">Bank Credit Timeline:</strong> The refund of <strong>₹%s</strong> has been issued back to your original payment method. Depending on your bank or UPI provider, it will reflect within 1&ndash;3 business days.
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <!-- Attached Receipt File Notice -->
                              <div style="background-color: #fbf7f4; border-left: 3px solid #d97706; border-radius: 6px; padding: 12px 14px; margin: 16px 0;">
                                <div style="font-size: 12px; line-height: 18px; color: #5a524c;">
                                  <strong style="color: #1e1b19;">📥 Download or Print Credit Note:</strong><br>
                                  1. An official credit note receipt file (<strong>Refund-Receipt-%s.html</strong>) is <strong>attached directly to this email</strong>.<br>
                                  2. You can also print or save as PDF right now by pressing <strong>Ctrl+P (or ⌘P)</strong> in your email client.
                                </div>
                              </div>

                              <p style="margin: 18px 0 0 0; font-size: 13px; line-height: 20px; color: #8a817a;">
                                In accordance with platform policy, enrollment access to this course has been revoked. You can explore other courses anytime on Focotech.
                              </p>
                            </td>
                          </tr>

                          <!-- Divider -->
                          <tr>
                            <td style="padding: 0 32px;">
                              <div style="border-top: 1px solid #eee7e2;"></div>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding: 20px 32px; background-color: #faf7f4; text-align: left;">
                              <p style="margin: 0 0 4px 0; font-size: 12px; color: #5a524c; font-weight: 600;">
                                Focotech &bull; Master In-Demand Tech Skills
                              </p>
                              <p style="margin: 0; font-size: 11px; color: #8a817a; line-height: 16px;">
                                &copy; 2026 Focotech. All rights reserved. &bull; Automated message, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        name, courseTitle, rfdId, dateStr,
                        name, toEmail, ordId, rfdId,
                        courseTitle, safeRefund.toPlainString(),
                        refundReason, safeRefund.toPlainString(), rfdId
                );
    }

    /**
     * Builds a standalone printable HTML Tax Invoice file for direct attachment.
     */
    private String buildStandaloneReceiptHtml(String name, String email, String courseTitle,
                                              String invoiceNumber, String transactionId,
                                              String orderId, BigDecimal amount) {
        BigDecimal safeAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? amount : new BigDecimal("499.00");
        BigDecimal baseAmount = safeAmount.divide(taxDivisor(orderId), 2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = safeAmount.subtract(baseAmount);
        BigDecimal cgst = gstAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = gstAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        String invNum = (invoiceNumber != null && !invoiceNumber.isBlank()) ? invoiceNumber : "INV-202611-" + (orderId != null && orderId.length() >= 6 ? orderId.substring(orderId.length() - 6) : "000000");
        String txnId = (transactionId != null && !transactionId.isBlank()) ? transactionId : "TXN-" + System.currentTimeMillis();
        String ordId = (orderId != null && !orderId.isBlank()) ? orderId : "OLP-ORD";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Tax Invoice - %s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px; background: #fcf6f2; color: #1e1916; }
                    .action-bar { max-width: 680px; margin: 0 auto 20px auto; display: flex; justify-content: flex-end; gap: 10px; }
                    .btn-action { padding: 9px 16px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; border: none; }
                    .btn-print { background: #ff5a36; color: #fff; }
                    .invoice-card { max-width: 680px; margin: 0 auto; background: #fff; padding: 40px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #fae8dc; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #ff5a36; padding-bottom: 20px; margin-bottom: 24px; }
                    .logo { font-size: 24px; font-weight: 900; }
                    .logo span { color: #ff5a36; }
                    .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; font-size: 13px; line-height: 1.6; }
                    table { width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-size: 13px; }
                    th { text-align: left; padding: 10px; background: #faf6f2; color: #6d635c; border-bottom: 1px solid #f0dfd3; }
                    td { padding: 12px 10px; border-bottom: 1px solid #f0dfd3; }
                    .summary { margin-left: auto; width: 280px; font-size: 13px; }
                    .summary-row { display: flex; justify-content: space-between; padding: 4px 0; color: #6d635c; }
                    .total-row { display: flex; justify-content: space-between; padding: 10px 0; border-top: 2px solid #1e1916; font-size: 16px; font-weight: 800; }
                    @media print { body { background: #fff; padding: 0; } .action-bar { display: none !important; } .invoice-card { box-shadow: none; border: none; padding: 0; } }
                  </style>
                </head>
                <body>
                  <div class="action-bar">
                    <button type="button" onclick="window.print()" class="btn-action btn-print">🖨️ Print / Save as PDF</button>
                  </div>
                  <div class="invoice-card">
                    <div class="header">
                      <div>
                        <div class="logo">FOCO<span>TECH</span></div>
                        <p style="margin: 4px 0 0; font-size: 11px; color: #8e8076;">Online Learning Technologies Pvt. Ltd.<br>GSTIN: 24AAACF1234F1Z5 | HSN: 999293</p>
                      </div>
                      <div style="text-align: right;">
                        <span style="background: #fff0eb; color: #ff5a36; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold;">OFFICIAL TAX INVOICE</span>
                        <h2 style="margin: 8px 0 2px; font-size: 16px; font-weight: 800;">%s</h2>
                        <p style="margin: 0; font-size: 11px; color: #8e8076;">Issued: %s</p>
                      </div>
                    </div>
                    <div class="details-grid">
                      <div>
                        <h4 style="margin: 0 0 4px; font-size: 11px; color: #8e8076; text-transform: uppercase;">Billed To</h4>
                        <strong>%s</strong><br>%s
                      </div>
                      <div style="text-align: right;">
                        <h4 style="margin: 0 0 4px; font-size: 11px; color: #8e8076; text-transform: uppercase;">Payment Details</h4>
                        Order Ref: <code>%s</code><br>Txn ID: <code>%s</code>
                      </div>
                    </div>
                    <table>
                      <thead>
                        <tr>
                          <th>Course Description</th>
                          <th style="text-align: center;">HSN</th>
                          <th style="text-align: right;">Amount (INR)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td><strong>%s</strong><br><span style="font-size: 11px; color: #8e8076;">Full Lifetime Course Access with Verified Certificate</span></td>
                          <td style="text-align: center;">999293</td>
                          <td style="text-align: right; font-weight: 700;">₹%s</td>
                        </tr>
                      </tbody>
                    </table>
                    <div class="summary">
                      <div class="summary-row"><span>Taxable Base Value:</span><span>₹%s</span></div>
                      <div class="summary-row"><span>CGST:</span><span>₹%s</span></div>
                      <div class="summary-row"><span>SGST:</span><span>₹%s</span></div>
                      <div class="total-row"><span>Total Paid:</span><span>₹%s</span></div>
                    </div>
                    <div style="margin-top: 30px; padding-top: 16px; border-top: 1px dashed #e8dcd5; font-size: 11px; color: #8e8076; text-align: center;">
                      This is a computer-generated official tax invoice. Authorized by Focotech Payment Subsystem.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                        invNum, invNum, dateStr,
                        name, email, ordId, txnId,
                        courseTitle, safeAmount.toPlainString(),
                        baseAmount.toPlainString(), cgst.toPlainString(), sgst.toPlainString(), safeAmount.toPlainString()
                );
    }

    /**
     * Builds a standalone printable HTML Refund Credit Note file for direct attachment.
     */
    private String buildStandaloneRefundReceiptHtml(String name, String email, String courseTitle,
                                                    String orderId, String refundId, BigDecimal refundAmount,
                                                    String refundReason) {
        BigDecimal safeRefund = (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) ? refundAmount : new BigDecimal("499.00");
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        String ordId = (orderId != null && !orderId.isBlank()) ? orderId : "OLP-ORD";
        String rfdId = (refundId != null && !refundId.isBlank()) ? refundId : "RFD-" + System.currentTimeMillis();

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Refund Credit Note - %s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 40px; background: #fcf6f2; color: #1e1916; }
                    .action-bar { max-width: 680px; margin: 0 auto 20px auto; display: flex; justify-content: flex-end; gap: 10px; }
                    .btn-action { padding: 9px 16px; border-radius: 8px; font-size: 13px; font-weight: 700; cursor: pointer; border: none; }
                    .btn-print { background: #d97706; color: #fff; }
                    .invoice-card { max-width: 680px; margin: 0 auto; background: #fff; padding: 40px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #fae8dc; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #d97706; padding-bottom: 20px; margin-bottom: 24px; }
                    .logo { font-size: 24px; font-weight: 900; }
                    .logo span { color: #ff5a36; }
                    .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; font-size: 13px; line-height: 1.6; }
                    table { width: 100%%; border-collapse: collapse; margin-bottom: 24px; font-size: 13px; }
                    th { text-align: left; padding: 10px; background: #fffbf5; color: #6d635c; border-bottom: 1px solid #f0dfd3; }
                    td { padding: 12px 10px; border-bottom: 1px solid #f0dfd3; }
                    @media print { body { background: #fff; padding: 0; } .action-bar { display: none !important; } .invoice-card { box-shadow: none; border: none; padding: 0; } }
                  </style>
                </head>
                <body>
                  <div class="action-bar">
                    <button type="button" onclick="window.print()" class="btn-action btn-print">🖨️ Print / Save as PDF</button>
                  </div>
                  <div class="invoice-card">
                    <div class="header">
                      <div>
                        <div class="logo">FOCO<span>TECH</span></div>
                        <p style="margin: 4px 0 0; font-size: 11px; color: #8e8076;">Online Learning Technologies Pvt. Ltd.<br>GSTIN: 24AAACF1234F1Z5</p>
                      </div>
                      <div style="text-align: right;">
                        <span style="background: #fef3c7; color: #b45309; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold;">OFFICIAL CREDIT NOTE</span>
                        <h2 style="margin: 8px 0 2px; font-size: 16px; font-weight: 800;">CN-%s</h2>
                        <p style="margin: 0; font-size: 11px; color: #8e8076;">Issued: %s</p>
                      </div>
                    </div>
                    <div class="details-grid">
                      <div>
                        <h4 style="margin: 0 0 4px; font-size: 11px; color: #8e8076; text-transform: uppercase;">Recipient</h4>
                        <strong>%s</strong><br>%s
                      </div>
                      <div style="text-align: right;">
                        <h4 style="margin: 0 0 4px; font-size: 11px; color: #8e8076; text-transform: uppercase;">Reversal Reference</h4>
                        Original Order: <code>%s</code><br>Refund Ref: <code>%s</code>
                      </div>
                    </div>
                    <table>
                      <thead>
                        <tr>
                          <th>Course Description</th>
                          <th style="text-align: center;">Policy</th>
                          <th style="text-align: right;">Refund Amount (INR)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td><strong>%s</strong><br><span style="font-size: 11px; color: #8e8076;">Course Access Revoked under 100%% Money-Back Guarantee</span></td>
                          <td style="text-align: center; color: #047857; font-weight: bold;">100%% Guarantee</td>
                          <td style="text-align: right; font-weight: 800; color: #047857; font-size: 15px;">₹%s</td>
                        </tr>
                      </tbody>
                    </table>
                    <div style="background: #faf7f4; padding: 12px; border-radius: 8px; font-size: 12px; margin-bottom: 20px;">
                      <strong>Reason for Refund:</strong> %s<br>
                      <strong>Credit Timeline:</strong> Refund credited back to original payment source (1&ndash;3 business days).
                    </div>
                    <div style="margin-top: 20px; padding-top: 16px; border-top: 1px dashed #e8dcd5; font-size: 11px; color: #8e8076; text-align: center;">
                      Official Computer-Generated Credit Note &bull; Authorized by Focotech Refund Subsystem.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                        rfdId, rfdId, dateStr,
                        name, email, ordId, rfdId,
                        courseTitle, safeRefund.toPlainString(),
                        refundReason
                );
    }

    private String buildPaymentFailedHtml(String name, String courseTitle, String orderId, String failureReason) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Payment Alert</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f7f4f1; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;">
                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f4f1; padding: 48px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 520px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e8e2dc; box-shadow: 0 4px 16px rgba(30, 27, 25, 0.04); overflow: hidden;">
                          
                          <tr>
                            <td style="background-color: #dc2626; height: 4px;"></td>
                          </tr>

                          <tr>
                            <td style="padding: 36px 40px 12px 40px; text-align: left;">
                              <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e1b19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                FOCO<span style="color: #ff5a36;">TECH</span>
                              </div>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding: 12px 40px 32px 40px;">
                              <h1 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 700; color: #1e1b19; letter-spacing: -0.3px;">
                                Payment Authorization Alert
                              </h1>
                              
                              <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 24px; color: #443e3a;">
                                Hello, <strong style="color: #1e1b19;">%s</strong>
                              </p>
                              
                              <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 24px; color: #443e3a;">
                                We were unable to complete your enrollment purchase for <strong style="color: #1e1b19;">"%s"</strong>.
                              </p>

                              <div style="background-color: #faf7f4; border: 1px solid #eae3dc; border-radius: 8px; padding: 20px 24px; text-align: left; margin: 24px 0 16px 0;">
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 13px; line-height: 22px; color: #443e3a;">
                                  <tr>
                                    <td style="color: #786f68; padding: 4px 0;">Order Ref:</td>
                                    <td style="text-align: right; font-family: monospace; font-weight: 600; color: #5a524c; padding: 4px 0;">%s</td>
                                  </tr>
                                  <tr>
                                    <td style="color: #786f68; padding: 4px 0;">Status:</td>
                                    <td style="text-align: right; font-weight: 700; color: #b91c1c; padding: 4px 0;">Unsuccessful / Cancelled</td>
                                  </tr>
                                  <tr>
                                    <td style="color: #786f68; padding: 4px 0;">Details:</td>
                                    <td style="text-align: right; color: #5a524c; padding: 4px 0;">%s</td>
                                  </tr>
                                </table>
                              </div>

                              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #faf7f4; border-left: 3px solid #ff5a36; border-radius: 4px; padding: 12px 14px; margin: 24px 0 20px 0;">
                                <tr>
                                  <td>
                                    <p style="margin: 0; font-size: 12px; line-height: 18px; color: #5a524c;">
                                      <strong style="color: #1e1b19;">Zero Deduction Guarantee:</strong> No money was deducted from your account. Your course access was not activated, and you may retry anytime.
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin: 20px 0 0 0; font-size: 13px; line-height: 20px; color: #8a817a;">
                                If you experienced an issue with your payment, you can log in to your dashboard and retry with the QR code.
                              </p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding: 0 40px;">
                              <div style="border-top: 1px solid #eee7e2;"></div>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding: 24px 40px; background-color: #faf7f4; text-align: left;">
                              <p style="margin: 0 0 4px 0; font-size: 12px; color: #5a524c; font-weight: 600;">
                                Focotech &bull; Master In-Demand Tech Skills
                              </p>
                              <p style="margin: 0; font-size: 11px; color: #8a817a; line-height: 16px;">
                                &copy; 2026 Focotech. All rights reserved. &bull; Automated message, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(name, courseTitle, orderId, failureReason);
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
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 520px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e8e2dc; box-shadow: 0 4px 16px rgba(30, 27, 25, 0.04); overflow: hidden;">
                          
                          <tr>
                            <td style="background-color: #ff5a36; height: 4px;"></td>
                          </tr>

                          <tr>
                            <td style="padding: 36px 40px 12px 40px; text-align: left;">
                              <div style="font-size: 22px; font-weight: 800; letter-spacing: -0.5px; color: #1e1b19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                FOCO<span style="color: #ff5a36;">TECH</span>
                              </div>
                            </td>
                          </tr>

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

                              <div style="background-color: #faf7f4; border: 1px solid #eae3dc; border-radius: 8px; padding: 24px 16px; text-align: center; margin: 24px 0 16px 0;">
                                <div style="font-family: 'SFMono-Regular', Consolas, Menlo, Monaco, monospace; font-size: 38px; font-weight: 700; letter-spacing: 10px; color: #1e1b19; padding-left: 10px;">
                                  %s
                                </div>
                                <div style="margin-top: 10px; font-size: 12px; font-weight: 500; color: #786f68;">
                                  Valid for %d minutes &bull; One-time use only
                                </div>
                              </div>

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

                          <tr>
                            <td style="padding: 0 40px;">
                              <div style="border-top: 1px solid #eee7e2;"></div>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding: 24px 40px; background-color: #faf7f4; text-align: left;">
                              <p style="margin: 0 0 4px 0; font-size: 12px; color: #5a524c; font-weight: 600;">
                                Focotech &bull; Master In-Demand Tech Skills
                              </p>
                              <p style="margin: 0; font-size: 11px; color: #8a817a; line-height: 16px;">
                                &copy; 2026 Focotech. All rights reserved. &bull; Automated message, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, purpose, email, otpCode, expiryMinutes);
    }
}

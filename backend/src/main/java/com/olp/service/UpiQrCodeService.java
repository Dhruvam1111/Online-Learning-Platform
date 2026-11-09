package com.olp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.olp.dto.UpiQrResponse;
import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * QR Code Generation Service for Payment Gateway (FR-PG01, FR-PG03).
 * Generates a scannable QR code encoding the payment reference details.
 * Uses Google ZXing for QR matrix rendering.
 */
@Service
public class UpiQrCodeService {

    private static final Logger log = LoggerFactory.getLogger(UpiQrCodeService.class);
    private static final String PAYEE_NAME = "Online Learning Platform";

    private final PaymentRepository paymentRepository;

    public UpiQrCodeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Generates a payment QR code for the given order.
     * The QR encodes a human-readable payment reference string.
     */
    @Transactional(readOnly = true)
    public UpiQrResponse generateUpiQr(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));

        // Enforce ownership: another student cannot view someone else's payment QR
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null && !"anonymousUser".equals(auth.getPrincipal())) {
            assertOwnership(auth, payment);
        }

        // Simple payment reference string — no external payment gateway required
        String qrContent = String.format(
                "OLP PAYMENT\nPayee: %s\nOrder: %s\nCourse: %s\nAmount: INR %.2f\nScan & show to confirm payment",
                PAYEE_NAME,
                payment.getOrderId(),
                payment.getCourse().getTitle(),
                payment.getAmount()
        );

        byte[] qrImageBytes = renderQrPngBytes(qrContent, 380, 380);
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrImageBytes);

        LocalDateTime expiresAt = payment.getCreatedAt().plusMinutes(10);

        log.info("[QR Engine] Generated payment QR for Order {} (₹{})", orderId, payment.getAmount());

        return new UpiQrResponse(
                payment.getOrderId(),
                qrContent,
                base64Image,
                payment.getAmount(),
                payment.getCurrency(),
                null,
                PAYEE_NAME,
                expiresAt
        );
    }

    /**
     * Returns raw PNG bytes for direct browser image viewing.
     */
    @Transactional(readOnly = true)
    public byte[] getQrPngBytes(String orderId) {
        UpiQrResponse qrData = generateUpiQr(orderId);
        return renderQrPngBytes(qrData.getUpiUri(), 400, 400);
    }

    private void assertOwnership(org.springframework.security.core.Authentication auth, Payment payment) {
        if (auth == null || payment == null) return;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_admin") || a.getAuthority().equalsIgnoreCase("admin"));
        if (isAdmin) return;

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            if ("admin".equalsIgnoreCase(user.getRole())) return;
            if (payment.getStudent() != null) {
                if (user.getId() != null && user.getId().equals(payment.getStudent().getId())) return;
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(payment.getStudent().getEmail())) return;
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You cannot view QR for another student's order.");
            }
        }
        String name = auth.getName();
        if (name != null && payment.getStudent() != null && payment.getStudent().getEmail() != null) {
            if (name.equalsIgnoreCase(payment.getStudent().getEmail())) return;
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You cannot view QR for another student's order.");
        }
    }

    /**
     * Renders a BitMatrix into a standard PNG byte array using Google ZXing.
     */
    private byte[] renderQrPngBytes(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("[QR Engine] Failed to generate QR code", e);
            throw new RuntimeException("Could not generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Renders an interactive "Scan & Pay" checkout portal page with live status polling.
     * Demonstrates the full payment flow: QR shown → payment confirmed → course enrolled.
     */
    @Transactional(readOnly = true)
    public String generateScanAndPayPortalHtml(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + orderId));

        UpiQrResponse qrData = generateUpiQr(orderId);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Scan & Pay — %s</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                body { background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
                .card { background: #1e293b; border: 1px solid #334155; border-radius: 20px; max-width: 440px; width: 100%%; padding: 28px; text-align: center; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); }
                .badge { display: inline-block; background: #0ea5e9; color: #fff; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; padding: 4px 12px; border-radius: 9999px; margin-bottom: 12px; }
                h1 { font-size: 20px; font-weight: 700; margin-bottom: 6px; color: #ffffff; }
                .subtitle { font-size: 13px; color: #94a3b8; margin-bottom: 20px; }
                .amount-pill { background: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 12px 16px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
                .amount-label { font-size: 13px; color: #94a3b8; }
                .amount-val { font-size: 22px; font-weight: 800; color: #38bdf8; }
                .qr-container { background: #ffffff; padding: 16px; border-radius: 16px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.3); margin-bottom: 16px; }
                .qr-container img { display: block; max-width: 260px; height: auto; border-radius: 8px; }
                .timer { font-size: 13px; color: #f59e0b; margin-bottom: 20px; font-weight: 600; }
                .btn { display: block; width: 100%%; padding: 12px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; border: none; margin-bottom: 10px; transition: 0.2s; }
                .btn-success { background: #10b981; color: #ffffff; }
                .btn-success:hover { background: #059669; }
                .btn-danger { background: #ef4444; color: #ffffff; }
                .btn-danger:hover { background: #dc2626; }
                .status-box { margin-top: 16px; padding: 10px; border-radius: 8px; font-size: 12px; background: #0f172a; border: 1px dashed #475569; color: #94a3b8; }
                .success-banner { display: none; background: #064e3b; border: 1px solid #10b981; color: #6ee7b7; padding: 14px; border-radius: 12px; margin-top: 15px; font-size: 14px; font-weight: 600; }
                .fail-banner { display: none; background: #450a0a; border: 1px solid #ef4444; color: #fca5a5; padding: 14px; border-radius: 12px; margin-top: 15px; font-size: 14px; font-weight: 600; }
                .order-ref { font-size: 11px; color: #64748b; margin-top: 8px; letter-spacing: 0.5px; }
              </style>
            </head>
            <body>
              <div class="card">
                <span class="badge">OLP Secure Payment</span>
                <h1>Scan QR to Pay</h1>
                <p class="subtitle">%s</p>

                <div class="amount-pill">
                  <span class="amount-label">Payable Amount</span>
                  <span class="amount-val">₹%.2f</span>
                </div>

                <div class="qr-container">
                  <img src="%s" alt="Payment QR Code" id="qrImg" />
                </div>

                <div class="timer" id="timer">Expires in: 09:59</div>
                <p class="order-ref">Order Ref: %s</p>

                <br/>

                <button onclick="confirmPayment()" class="btn btn-success">✅ Simulate Payment Success (Viva Demo)</button>
                <button onclick="failPayment()" class="btn btn-danger">❌ Simulate Payment Failure</button>

                <div class="status-box" id="statusBox">
                  Waiting for payment confirmation on Order: <strong>%s</strong>...
                </div>

                <div class="success-banner" id="successBanner">
                  Payment received! Course access unlocked. Redirecting...
                </div>

                <div class="fail-banner" id="failBanner">
                  Payment failed. No enrollment created. You may retry.
                </div>
              </div>

              <script>
                const orderId = "%s";
                let remaining = 600;
                let done = false;

                const timerInterval = setInterval(() => {
                  if (done) return;
                  remaining--;
                  if (remaining <= 0) {
                    clearInterval(timerInterval);
                    document.getElementById('timer').innerText = "QR Code Expired. Please create a new order.";
                    document.getElementById('timer').style.color = "#ef4444";
                    return;
                  }
                  const mins = String(Math.floor(remaining / 60)).padStart(2, '0');
                  const secs = String(remaining %% 60).padStart(2, '0');
                  document.getElementById('timer').innerText = "Expires in: " + mins + ":" + secs;
                }, 1000);

                // Auto-polling for payment status
                const pollInterval = setInterval(() => {
                  if (done) { clearInterval(pollInterval); return; }
                  fetch('/api/payments/status/' + orderId)
                    .then(res => res.json())
                    .then(data => {
                      if (data.status === 'success') {
                        done = true;
                        clearInterval(pollInterval);
                        clearInterval(timerInterval);
                        document.getElementById('statusBox').style.display = 'none';
                        document.getElementById('timer').style.display = 'none';
                        const banner = document.getElementById('successBanner');
                        banner.style.display = 'block';
                        banner.innerText = "Payment confirmed! Invoice: " + (data.invoiceNumber || 'Generated') + " — Enrolled in " + data.courseTitle + "!";
                      } else if (data.status === 'failed') {
                        done = true;
                        clearInterval(pollInterval);
                        clearInterval(timerInterval);
                        document.getElementById('statusBox').style.display = 'none';
                        document.getElementById('failBanner').style.display = 'block';
                      }
                    })
                    .catch(e => console.error("Poll error", e));
                }, 2000);

                function confirmPayment() {
                  const btn = event.target;
                  btn.innerText = "Processing payment...";
                  btn.disabled = true;
                  document.querySelector('.btn-danger').disabled = true;
                  fetch('/api/payments/confirm/' + orderId, { method: 'POST' })
                    .then(res => res.json())
                    .then(data => {
                      if (data.status === 'success') {
                        done = true;
                        clearInterval(pollInterval);
                        clearInterval(timerInterval);
                        document.getElementById('statusBox').style.display = 'none';
                        document.getElementById('timer').style.display = 'none';
                        const banner = document.getElementById('successBanner');
                        banner.style.display = 'block';
                        banner.innerHTML = '✅ Payment confirmed!<br>Invoice: <strong>' + (data.invoiceNumber || 'Generated') + '</strong><br>Enrolled in <strong>' + data.courseTitle + '</strong><br><small>Redirecting to dashboard in 3s...</small>';
                        setTimeout(() => {
                          window.location.href = 'http://localhost:3000/#dashboard';
                        }, 2500);
                      } else {
                        alert("Unexpected status: " + data.status);
                        btn.disabled = false;
                        btn.innerText = "✅ Simulate Payment Success (Viva Demo)";
                      }
                    })
                    .catch(err => {
                      alert("Error: " + err);
                      btn.disabled = false;
                      btn.innerText = "✅ Simulate Payment Success (Viva Demo)";
                      document.querySelector('.btn-danger').disabled = false;
                    });
                }

                function failPayment() {
                  const btn = event.target;
                  btn.innerText = "Processing...";
                  btn.disabled = true;
                  document.querySelector('.btn-success').disabled = true;
                  fetch('/api/payments/fail/' + orderId, { method: 'POST' })
                    .then(res => res.json())
                    .then(data => {
                      done = true;
                      clearInterval(pollInterval);
                      clearInterval(timerInterval);
                      document.getElementById('statusBox').style.display = 'none';
                      document.getElementById('timer').style.display = 'none';
                      const failBanner = document.getElementById('failBanner');
                      failBanner.style.display = 'block';
                      failBanner.innerHTML = '❌ Payment failed. No enrollment created.<br><small>Returning to courses in 3s...</small>';
                      setTimeout(() => {
                        window.location.href = 'http://localhost:3000/#dashboard';
                      }, 2500);
                    })
                    .catch(err => {
                      alert("Error: " + err);
                      btn.disabled = false;
                      btn.innerText = "❌ Simulate Payment Failure";
                      document.querySelector('.btn-success').disabled = false;
                    });
                }
              </script>
            </body>
            </html>
            """.formatted(
                payment.getCourse().getTitle(),
                payment.getCourse().getTitle(),
                payment.getAmount(),
                qrData.getQrBase64(),
                orderId,
                orderId,
                orderId
        );
    }
}

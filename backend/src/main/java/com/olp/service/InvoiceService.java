package com.olp.service;

import com.olp.dto.InvoiceResponse;
import com.olp.entity.Payment;
import com.olp.entity.User;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.PaymentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Service generating official GST-compliant tax invoices and receipts (NFR-23, NFR-33).
 * Owned by Dhruvam (Invoice & Tax Auditor).
 */
@Service
public class InvoiceService {

    private final PaymentRepository paymentRepository;

    public InvoiceService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceData(User requester, String orderId) {
        String query = orderId != null ? orderId.trim() : "";
        Payment payment = paymentRepository.findByOrderId(query)
                .or(() -> paymentRepository.findByOrderIdIgnoreCase(query))
                .or(() -> paymentRepository.findByInvoiceNumber(query))
                .or(() -> paymentRepository.findByPaymentId(query))
                .orElseThrow(() -> new ResourceNotFoundException("Payment receipt not found for identifier: " + orderId));

        if (requester == null) {
            throw new AccessDeniedException("Authentication required to access invoice.");
        }

        boolean isAdmin = "admin".equalsIgnoreCase(requester.getRole());
        boolean isOwner = payment.getStudent() != null && payment.getStudent().getId().equals(requester.getId());
        boolean isInstructor = payment.getCourse() != null && payment.getCourse().getInstructor() != null
                && payment.getCourse().getInstructor().getId().equals(requester.getId());

        if (!isAdmin && !isOwner && !isInstructor) {
            throw new AccessDeniedException("Unauthorized: You do not have permission to access this invoice.");
        }

        BigDecimal walletPaid = payment.getWalletAmountUsed() != null ? payment.getWalletAmountUsed() : BigDecimal.ZERO;
        BigDecimal total = payment.getAmount().add(walletPaid);
        BigDecimal base = total.divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(base);

        String instructorName = payment.getCourse().getInstructor() != null
                ? payment.getCourse().getInstructor().getName()
                : "Platform Instructor";

        return new InvoiceResponse(
                payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "INV-2026-" + payment.getId(),
                payment.getOrderId(),
                payment.getPaymentId() != null ? payment.getPaymentId() : "PENDING",
                payment.getStudent().getId(),
                payment.getStudent().getName(),
                payment.getStudent().getEmail(),
                payment.getCourse().getId(),
                payment.getCourse().getTitle(),
                instructorName,
                base,
                gst,
                total,
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getCreatedAt()
        );
    }

    /**
     * Generates a printable HTML tax receipt with official company styling.
     */
    @Transactional(readOnly = true)
    public String generateInvoiceHtml(User requester, String orderId) {
        InvoiceResponse inv = getInvoiceData(requester, orderId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>Tax Invoice - %s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 40px; background: #fcf6f2; color: #1e1916; }
                    .action-bar { max-width: 700px; margin: 0 auto 20px auto; display: flex; justify-content: flex-end; gap: 12px; }
                    .btn-action { display: inline-flex; align-items: center; gap: 6px; padding: 10px 18px; border-radius: 12px; font-size: 13px; font-weight: 700; text-decoration: none; cursor: pointer; border: none; transition: 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
                    .btn-print { background: #ff5a36; color: #fff; }
                    .btn-print:hover { background: #e5411d; }
                    .btn-download { background: #1e1916; color: #fff; }
                    .btn-download:hover { background: #000; }
                    .invoice-card { max-width: 700px; margin: 0 auto; background: #fff; padding: 48px; border-radius: 20px; box-shadow: 0 4px 24px rgba(0,0,0,0.06); border: 1px solid #fae8dc; }
                    .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #ff5a36; padding-bottom: 24px; margin-bottom: 28px; }
                    .logo { font-size: 24px; font-weight: 900; letter-spacing: -0.5px; }
                    .logo span { color: #ff5a36; }
                    .tax-badge { font-size: 11px; font-weight: bold; background: #fff0eb; color: #ff5a36; padding: 4px 10px; border-radius: 20px; text-transform: uppercase; }
                    .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 32px; font-size: 13px; line-height: 1.6; }
                    .details-box h4 { margin: 0 0 6px 0; font-size: 11px; text-transform: uppercase; color: #9d8d85; letter-spacing: 0.5px; }
                    table { width: 100%%; border-collapse: collapse; margin-bottom: 32px; font-size: 13px; }
                    th { text-align: left; padding: 12px; background: #faf6f2; color: #6d635c; font-weight: 700; border-bottom: 1px solid #f0dfd3; }
                    td { padding: 14px 12px; border-bottom: 1px solid #f0dfd3; }
                    .summary { margin-left: auto; width: 280px; font-size: 13px; }
                    .summary-row { display: flex; justify-content: space-between; padding: 6px 0; color: #6d635c; }
                    .total-row { display: flex; justify-content: space-between; padding: 12px 0; border-top: 2px solid #1e1916; font-size: 16px; font-weight: 800; color: #1e1916; }
                    .footer { margin-top: 40px; padding-top: 20px; border-top: 1px dashed #e8dcd5; font-size: 11px; color: #8e8076; text-align: center; }
                    @media print { 
                      body { background: #fff; padding: 0; } 
                      .action-bar { display: none !important; }
                      .invoice-card { box-shadow: none; border: none; padding: 20px; } 
                    }
                  </style>
                </head>
                <body>
                  <div class="action-bar">
                    <button type="button" onclick="window.print()" class="btn-action btn-print">🖨️ Print / Save as PDF</button>
                    <a href="/api/payments/invoices/%s/download" onclick="return downloadReceiptFile(event)" download class="btn-action btn-download">📥 Download Receipt (.html)</a>
                  </div>
                  <div class="invoice-card">
                    <div class="header">
                      <div>
                        <div class="logo">FOCO<span>TECH</span></div>
                        <p style="margin: 4px 0 0; font-size: 12px; color: #8e8076;">Online Learning Technologies Pvt. Ltd.<br>GSTIN: %s | HSN: %s</p>
                      </div>
                      <div style="text-align: right;">
                        <span class="tax-badge">Official Tax Invoice</span>
                        <h2 style="margin: 10px 0 2px; font-size: 18px; font-weight: 800;">%s</h2>
                        <p style="margin: 0; font-size: 11px; color: #8e8076;">Issued: %s</p>
                      </div>
                    </div>
                
                    <div class="details-grid">
                      <div class="details-box">
                        <h4>Billed To (Student)</h4>
                        <strong style="font-size: 14px;">%s</strong><br>
                        Email: %s<br>
                        Student ID: #%s
                      </div>
                      <div class="details-box" style="text-align: right;">
                        <h4>Payment Details</h4>
                        Order Ref: <code>%s</code><br>
                        Transaction ID: <code>%s</code><br>
                        Payment Method: <strong>%s</strong>
                      </div>
                    </div>
                
                    <table>
                      <thead>
                        <tr>
                          <th>Item Description</th>
                          <th>Instructor</th>
                          <th>HSN/SAC</th>
                          <th style="text-align: right;">Amount (INR)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td><strong>%s</strong><br><span style="font-size: 11px; color: #8e8076;">Full Lifetime Course Access with Verified Certificate</span></td>
                          <td>%s</td>
                          <td>%s</td>
                          <td style="text-align: right; font-weight: 700;">₹%s</td>
                        </tr>
                      </tbody>
                    </table>
                
                    <div class="summary">
                      <div class="summary-row"><span>Taxable Base Value:</span><span>₹%s</span></div>
                      <div class="summary-row"><span>CGST (9.0%%):</span><span>₹%s</span></div>
                      <div class="summary-row"><span>SGST (9.0%%):</span><span>₹%s</span></div>
                      <div class="total-row"><span>Total Paid:</span><span>₹%s</span></div>
                    </div>
                
                    <div class="footer">
                      <p>This is a computer-generated tax invoice. No signature required.<br>
                      Authorized by Online Learning Platform Payment Engine • Verified Transaction</p>
                    </div>
                  </div>
                  <script>
                    function downloadReceiptFile(e) {
                      if (window.location.protocol === 'file:' || window.location.origin === 'null') {
                        try {
                          if (e) e.preventDefault();
                          var docHtml = '<!DOCTYPE html>\\n' + document.documentElement.outerHTML;
                          var blob = new Blob([docHtml], { type: 'text/html;charset=utf-8' });
                          var url = window.URL.createObjectURL(blob);
                          var a = document.createElement('a');
                          a.href = url;
                          a.download = (document.title || 'Receipt').replace(/[^a-zA-Z0-9_-]/g, '_') + '.html';
                          document.body.appendChild(a);
                          a.click();
                          setTimeout(function() {
                            document.body.removeChild(a);
                            window.URL.revokeObjectURL(url);
                          }, 250);
                          return false;
                        } catch (err) {}
                      }
                      return true;
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                inv.getInvoiceNumber(),
                inv.getOrderId(),
                inv.getGstin(),
                inv.getHsnCode(),
                inv.getInvoiceNumber(),
                inv.getIssuedAt().format(fmt),
                inv.getStudentName(),
                inv.getStudentEmail(),
                String.valueOf(inv.getStudentId()),
                inv.getOrderId(),
                inv.getPaymentId(),
                inv.getPaymentMethod(),
                inv.getCourseTitle(),
                inv.getInstructorName(),
                inv.getHsnCode(),
                inv.getTotalAmount().toPlainString(),
                inv.getBaseAmount().toPlainString(),
                inv.getGstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP).toPlainString(),
                inv.getGstAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP).toPlainString(),
                inv.getTotalAmount().toPlainString()
        );
    }
}

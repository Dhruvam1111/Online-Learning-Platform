package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.FinancialDocumentService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/payments")
public class FinancialDocumentController {
    private final FinancialDocumentService documents;
    public FinancialDocumentController(FinancialDocumentService documents){this.documents=documents;}
    @GetMapping(value="/instructor/payouts/{reference}/statement",produces=MediaType.TEXT_HTML_VALUE) public ResponseEntity<String> statement(@AuthenticationPrincipal User user,@PathVariable String reference){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"payout-statement.html\"").body(documents.payoutStatement(user,reference));}
    @GetMapping(value="/invoices/{orderId}/commission",produces=MediaType.TEXT_HTML_VALUE) public ResponseEntity<String> commission(@AuthenticationPrincipal User user,@PathVariable String orderId){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"commission-invoice.html\"").body(documents.commissionInvoice(user,orderId));}
}

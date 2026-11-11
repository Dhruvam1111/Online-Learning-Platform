package com.olp.service;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.util.HtmlUtils;
import java.math.*;
import java.util.*;
@Service
public class FinancialDocumentService {
    private final PaymentAuditService audit;
    private final PaymentRepository payments;
    private final PaymentPolicyService policies;
    public FinancialDocumentService(PaymentAuditService audit,PaymentRepository payments,PaymentPolicyService policies){this.audit=audit;this.payments=payments;this.policies=policies;}
    private String text(Object value){return HtmlUtils.htmlEscape(String.valueOf(value));}
    private String document(String title,Map<String,?> values){
        StringBuilder html=new StringBuilder("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"+text(title)+"</title></head><body><main><h1>"+text(title)+"</h1><p>Simulation document. No actual transfer or statutory filing is represented.</p><table><caption>Financial breakdown</caption><tbody>");
        values.forEach((key,value)->html.append("<tr><th scope=\"row\">").append(text(key)).append("</th><td>").append(text(value)).append("</td></tr>"));return html.append("</tbody></table></main></body></html>").toString();
    }
    @Transactional(readOnly=true) public String payoutStatement(User user,String reference){
        if(user==null || !Set.of("instructor","admin").contains(user.getRole()))throw new AccessDeniedException("Instructor or administrator required.");
        if(reference==null || !reference.matches("IMPS-SIM-[A-Za-z0-9-]{1,80}"))throw new IllegalArgumentException("Invalid payout reference.");
        var data=audit.get(reference);
        if(!"admin".equals(user.getRole()) && !user.getId().toString().equals(data.get("instructorId").toString()))throw new AccessDeniedException("This statement belongs to another instructor.");
        var fields=new LinkedHashMap<String,Object>();fields.put("Reference",reference);fields.put("Instructor ID",data.get("instructorId"));fields.put("Gross royalty INR",data.getOrDefault("grossRoyalty",data.get("amount")));fields.put("Withholding rate",new BigDecimal(data.getOrDefault("withholdingRate",BigDecimal.ZERO).toString()).multiply(new BigDecimal("100")).toPlainString()+"%");fields.put("Withholding INR",data.getOrDefault("withholdingAmount",BigDecimal.ZERO));fields.put("Refund debt recovered INR",data.getOrDefault("refundDebtRecovered",BigDecimal.ZERO));fields.put("Net payout INR",data.get("amount"));fields.put("Account",data.get("account"));fields.put("Recorded at",data.get("recordedAt"));return document("Instructor payout and withholding statement",fields);
    }
    @Transactional(readOnly=true) public String commissionInvoice(User user,String orderId){
        var p=payments.findByOrderId(orderId).orElseThrow(()->new IllegalArgumentException("Order not found."));
        if(user==null || !("admin".equals(user.getRole()) || (p.getCourse().getInstructor()!=null && p.getCourse().getInstructor().getId().equals(user.getId()))))throw new AccessDeniedException("Only the instructor or administrator may view this commission invoice.");
        if(p.getPlatformFee()==null || !Set.of("success","disputed","charged_back").contains(p.getStatus()))throw new IllegalStateException("Commission is available after settlement.");
        BigDecimal rate=new BigDecimal(policies.forOrder(orderId).getOrDefault("commissionTaxRate",new BigDecimal("0.18")).toString());BigDecimal total=p.getPlatformFee();BigDecimal base=total.divide(BigDecimal.ONE.add(rate),2,RoundingMode.HALF_UP);
        var values=new LinkedHashMap<String,Object>();values.put("Commission invoice","COM-"+p.getInvoiceNumber());values.put("Order",orderId);values.put("Instructor ID",p.getCourse().getInstructor()==null?"unassigned":p.getCourse().getInstructor().getId());values.put("Tax rate",rate.multiply(new BigDecimal("100")).toPlainString()+"%");values.put("Base commission INR",base);values.put("Tax INR",total.subtract(base));values.put("Total commission INR",total);values.put("Refund status",p.getRefundStatus()==null?"none":p.getRefundStatus());return document("Platform commission invoice",values);
    }
}

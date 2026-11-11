package com.olp.service;
import com.olp.entity.User;
import com.olp.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
@Service
public class PaymentMaintenanceService {
    private final EntityManager entityManager;
    private final PaymentAuditService audit;
    private final ReconciliationService reconciliation;
    private final PaymentRepository orders;
    private final PaymentService payments;
    public PaymentMaintenanceService(EntityManager entityManager,PaymentAuditService audit,ReconciliationService reconciliation,PaymentRepository orders,PaymentService payments){this.entityManager=entityManager;this.audit=audit;this.reconciliation=reconciliation;this.orders=orders;this.payments=payments;}
    @Transactional
    public void reconcileDaily(){
        if(!Boolean.TRUE.equals(entityManager.createNativeQuery("select pg_try_advisory_xact_lock(713140012)").getSingleResult()))return;
        String id="daily-reconciliation-"+LocalDate.now(ZoneOffset.UTC);
        if(audit.exists(id))return;
        User system=new User();system.setRole("admin");
        var result=reconciliation.generateReconciliationReport(system);
        audit.record(id,"reconciliation.daily",Map.of("report",result,"mode","simulation"));
    }
    public void recoverExpired(){
        for(String id:orders.findExpiredOrderIds(LocalDateTime.now().minusMinutes(PaymentService.ORDER_EXPIRY_MINUTES)))payments.getOrderStatus(id);
    }
}

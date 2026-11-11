package com.olp.service;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
/** Enable only after applying the payment hardening migration. Database locks prevent duplicate daily reports. */
@Component
@EnableScheduling
@ConditionalOnProperty(name="payments.jobs.enabled",havingValue="true")
public class PaymentMaintenanceJobs {
    private final PaymentMaintenanceService maintenance;
    private final PaymentSubscriptionService subscriptions;
    public PaymentMaintenanceJobs(PaymentMaintenanceService maintenance,PaymentSubscriptionService subscriptions){this.maintenance=maintenance;this.subscriptions=subscriptions;}
    @Scheduled(fixedDelayString="${payments.jobs.notice-delay-ms:60000}") public void notices(){subscriptions.prepareNotices();}
    @Scheduled(cron="${payments.jobs.reconciliation-cron:0 0 1 * * *}",zone="UTC") public void reconcile(){maintenance.reconcileDaily();}
}

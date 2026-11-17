package com.olp.service;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ensures timed-out exams are finalized even when the student closes their browser. */
@Component
@EnableScheduling
public class StudentExamTimeoutWorker {
    private final StudentExamLifecycleService service;
    public StudentExamTimeoutWorker(StudentExamLifecycleService service) { this.service = service; }
    @Scheduled(fixedDelay = 30000)
    public void finalizeExpired() { service.expireAllDue(); }
}

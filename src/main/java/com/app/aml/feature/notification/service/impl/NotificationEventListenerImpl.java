package com.app.aml.feature.notification.service.impl;


import com.app.aml.feature.notification.event.*;
import com.app.aml.feature.notification.service.interfaces.MailService;
import com.app.aml.feature.notification.service.interfaces.NotificationEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListenerImpl implements NotificationEventListener {

    private final MailService mailService;

    @Async
    @EventListener
    public void handleCaseAssigned(CaseAssignedEvent event) {
        log.info("Caught CaseAssignedEvent for Case: {}", event.getCaseReference());

        String subject = "New AML Case Assigned: " + event.getCaseReference();
        String body = String.format(
                "Hello,\n\nYou have been assigned to investigate case %s.\n" +
                        "Please log in to the AML portal to review the alert details and customer profile.\n\n" +
                        "Thank you,\nAML Platform Security",
                event.getCaseReference()
        );

        mailService.sendEmail(event.getNewAssigneeEmail(), subject, body);
    }

    @Async
    @EventListener
    public void handleBatchUploaded(BatchUploadedEvent event) {
        log.info("Caught BatchUploadedEvent for Batch ID: {}", event.getBatchId());

        String subject = "File Uploaded Successfully: Processing Started";
        String body = String.format(
                "Hello,\n\nYour transaction batch file has been securely uploaded and is now in the queue for ingestion.\n\n" +
                        "The Rule Engine is currently evaluating your transactions. You will receive another notification as soon as the processing is complete.\n\n" +
                        "Thank you,\nAML Platform Security"
        );

        mailService.sendEmail(event.getUploaderEmail(), subject, body);
    }

    @Async
    @EventListener
    public void handleBatchCompleted(BatchCompletedEvent event) {
        log.info("Caught BatchCompletedEvent for Batch: {}", event.getBatchReference());

        String subject = "Batch Processing Complete: " + event.getBatchReference();
        String body = String.format(
                "Hello,\n\nYour transaction batch %s has finished processing.\n\n" +
                        "Status: %s\nTotal Records: %d\nFailed Records: %d\n\n" +
                        "Please log in to the portal to view detailed results.\n\n" +
                        "Thank you,\nAML Platform Security",
                event.getBatchReference(), event.getStatus(), event.getTotalRecords(), event.getFailedRecords()
        );

        mailService.sendEmail(event.getUploaderEmail(), subject, body);
    }

    @Async
    @EventListener
    public void handleAccountLocked(AccountLockedEvent event) {
        log.info("Caught AccountLockedEvent for User: {}", event.getUserEmail());

        String subject = "SECURITY ALERT: Account Locked";
        String body = String.format(
                "Hello,\n\nYour AML platform account has been locked due to the following reason:\n%s\n\n" +
                        "If you did not initiate this action, please contact your Bank Administrator immediately.\n\n" +
                        "Thank you,\nAML Platform Security",
                event.getReason()
        );

        mailService.sendEmail(event.getUserEmail(), subject, body);
    }

    @Async
    @EventListener
    public void handleTenantCreated(TenantCreatedEvent event) {
        log.info("Caught TenantCreatedEvent for Tenant: {}", event.getTenantName());

        String subject = "Welcome to the AML Platform: " + event.getTenantName();
        String body = String.format(
                "Hello,\n\nYour institution (%s) has been successfully onboarded to the AML Platform.\n" +
                        "Bank Code: %s\n\n" +
                        "Your schema has been provisioned. Please use the temporary credentials provided separately to log in and begin configuring your rule thresholds.\n\n" +
                        "Thank you,\nPlatform Administration",
                event.getTenantName(), event.getBankCode()
        );

        mailService.sendEmail(event.getAdminEmail(), subject, body);
    }

    @Async
    @EventListener
    public void handleStrFiled(StrFiledEvent event) {
        log.info("Caught StrFiledEvent for STR: {}", event.getStrReference());

        String subject = "STR Filing Confirmation: " + event.getStrReference();
        String body = String.format(
                "Hello,\n\nThis is a confirmation that Suspicious Transaction Report %s has been successfully generated and filed.\n\n" +
                        "A copy of the PDF report is securely stored and linked to the original case.\n\n" +
                        "Thank you,\nAML Platform Compliance",
                event.getStrReference()
        );

        mailService.sendEmail(event.getFiledByEmail(), subject, body);
    }
    @Async
    @EventListener
    public void handleCaseEscalated(CaseEscalatedEvent event) {
        log.info("Caught CaseEscalatedEvent for Case: {}", event.getCaseReference());

        String subject = "ACTION REQUIRED: AML Case Escalated - " + event.getCaseReference();
        String body = String.format(
                "Hello,\n\nCase %s has been escalated to you for immediate review.\n\n" +
                        "Escalation Reason:\n%s\n\n" +
                        "Please log in to the AML portal to review the investigation details and determine the next steps.\n\n" +
                        "Thank you,\nAML Platform Security",
                event.getCaseReference(), event.getReason()
        );

        mailService.sendEmail(event.getAdminEmail(), subject, body);
    }
}
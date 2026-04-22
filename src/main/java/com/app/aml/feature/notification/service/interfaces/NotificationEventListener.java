package com.app.aml.feature.notification.service.interfaces;

import com.app.aml.feature.notification.event.*;

public interface NotificationEventListener {
    void handleCaseAssigned(CaseAssignedEvent event);
    void handleBatchUploaded(BatchUploadedEvent event);
    void handleBatchCompleted(BatchCompletedEvent event);
    void handleAccountLocked(AccountLockedEvent event);
    void handleTenantCreated(TenantCreatedEvent event);
    void handleStrFiled(StrFiledEvent event);
}

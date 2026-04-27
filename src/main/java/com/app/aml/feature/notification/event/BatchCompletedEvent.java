package com.app.aml.feature.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class BatchCompletedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID batchId;
    private final String batchReference;
    private final String uploaderEmail;
    private final String status;
    private final int totalRecords;
    private final int failedRecords;

    public BatchCompletedEvent(Object source,
                               String tenantId,
                               UUID batchId,
                               String batchReference,
                               String uploaderEmail,
                               String status,
                               int totalRecords,
                               int failedRecords) {
        super(source);
        this.tenantId = tenantId;
        this.batchId = batchId;
        this.batchReference = batchReference;
        this.uploaderEmail = uploaderEmail;
        this.status = status;
        this.totalRecords = totalRecords;
        this.failedRecords = failedRecords;
    }
}
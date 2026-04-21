package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when a Transaction Batch finishes processing.
 * Used to trigger success/failure notifications and audit logs.
 */
@Getter
public class BatchCompletedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID batchId;
    private final String batchReference;
    private final String uploaderEmail;
    private final String status; // e.g., "PROCESSED", "FAILED", "PARTIAL_SUCCESS"
    private final int totalRecords;
    private final int failedRecords;

    /**
     * @param source         The object on which the event initially occurred.
     * @param tenantId       The ID of the tenant/bank.
     * @param batchId        The database UUID of the batch.
     * @param batchReference The reference string (e.g., BATCH-20260421-001).
     * @param uploaderEmail  The email of the user who uploaded the file.
     * @param status         The final processing status.
     * @param totalRecords   The total count of rows in the CSV.
     * @param failedRecords  The count of rows that failed validation/ingestion.
     */
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
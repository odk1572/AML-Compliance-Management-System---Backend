package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published immediately after a raw CSV file is successfully uploaded
 * to storage. This signals the background worker to start the ingestion
 * and rule-scanning process.
 */
@Getter
public class BatchUploadedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID batchId;
    private final String fileUrl;
    private final String uploaderEmail;

    /**
     * @param source         The service that handled the upload.
     * @param tenantId       The ID of the tenant (for schema routing).
     * @param batchId        The UUID assigned to this new batch record.
     * @param fileUrl        The secure URL of the file in Cloudinary.
     * @param uploaderEmail  The user to notify if the initial parsing fails.
     */
    public BatchUploadedEvent(Object source,
                              String tenantId,
                              UUID batchId,
                              String fileUrl,
                              String uploaderEmail) {
        super(source);
        this.tenantId = tenantId;
        this.batchId = batchId;
        this.fileUrl = fileUrl;
        this.uploaderEmail = uploaderEmail;
    }
}
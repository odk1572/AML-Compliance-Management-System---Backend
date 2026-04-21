package com.app.aml.shared.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class BatchUploadedEvent extends ApplicationEvent {

    private final String tenantId;
    private final UUID batchId;
    private final String fileUrl;
    private final String uploaderEmail;

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
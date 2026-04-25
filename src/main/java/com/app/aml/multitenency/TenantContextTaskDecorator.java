package com.app.aml.multitenency;


import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantContextTaskDecorator implements TaskDecorator {
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capture context from the main (Batch) thread
        String tenantId = TenantContext.getTenantId();
        String schemaName = TenantContext.getSchemaName();

        return () -> {
            try {
                // Apply context to the worker thread
                if (tenantId != null) TenantContext.setTenantId(tenantId);
                if (schemaName != null) TenantContext.setSchemaName(schemaName);
                runnable.run();
            } finally {
                // Clear after work is done
                TenantContext.clear();
            }
        };
    }
}
package com.app.aml.multitenency;


import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantContextTaskDecorator implements TaskDecorator {
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        String tenantId = TenantContext.getTenantId();
        String schemaName = TenantContext.getSchemaName();

        return () -> {
            try {
                if (tenantId != null) TenantContext.setTenantId(tenantId);
                if (schemaName != null) TenantContext.setSchemaName(schemaName);
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
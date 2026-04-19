package com.app.aml.domain.valueObject;

import java.util.UUID;

/**
 * Value Object representing a strictly typed Tenant ID.
 * Prevents accidental mixing of raw String parameters in service methods.
 */
public record TenantId(String value) {

    /**
     * Compact constructor for validation.
     * Guaranteed to run whenever a TenantId is instantiated.
     */
    public TenantId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty.");
        }

        // Since your database uses UUIDs for the tenants table, we can enforce
        // the UUID format right here at the domain boundary.
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tenant ID must be a valid UUID format.", e);
        }
    }

    /**
     * Factory method for cleaner instantiation.
     */
    public static TenantId of(String value) {
        return new TenantId(value);
    }

    /**
     * Convenience method to get the UUID object if needed by JPA/JDBC layers.
     */
    public UUID asUuid() {
        return UUID.fromString(value);
    }

    /**
     * Overriding toString to return just the value.
     * This makes logging much cleaner (e.g., "b38...91a" instead of "TenantId[value=b38...91a]").
     */
    @Override
    public String toString() {
        return value;
    }
}
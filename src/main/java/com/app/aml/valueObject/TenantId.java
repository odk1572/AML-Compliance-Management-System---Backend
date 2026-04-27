package com.app.aml.valueObject;

import java.util.UUID;


public record TenantId(String value) {


    public TenantId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty.");
        }


        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tenant ID must be a valid UUID format.", e);
        }
    }


    public static TenantId of(String value) {
        return new TenantId(value);
    }


    public UUID asUuid() {
        return UUID.fromString(value);
    }


    @Override
    public String toString() {
        return value;
    }
}
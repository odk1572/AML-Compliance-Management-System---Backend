package com.app.aml.exceptions;


import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Getter
public class ResourceNotFoundException extends ApplicationException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, UUID resourceId) {
        // Pass message, Error Code, and HTTP 404 to the parent class
        super(
                String.format("%s not found with ID: '%s'", resourceType, resourceId),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId.toString();
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(
                String.format("%s not found with identifier: '%s'", resourceType, resourceId),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}

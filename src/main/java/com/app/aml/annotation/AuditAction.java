package com.app.aml.annotation;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String category();
    String action();
    String entityType() default "GENERAL";
}
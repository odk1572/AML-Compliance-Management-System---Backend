package com.app.aml.feature.notification.service.interfaces;

public interface MailService {
    void sendEmail(String to, String subject, String text);
}

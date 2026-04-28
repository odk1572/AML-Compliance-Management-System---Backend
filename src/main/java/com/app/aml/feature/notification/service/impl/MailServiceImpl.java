package com.app.aml.feature.notification.service.impl;

import com.app.aml.feature.notification.service.interfaces.MailService;
import com.app.aml.annotation.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    @AuditAction(category = "COMMUNICATION", action = "SEND_MANUAL_EMAIL", entityType = "NOTIFICATION")
    public void sendEmail(String to, String subject, String text) {
        try {
            log.debug("Attempting to send email to [{}] with subject [{}]", to, subject);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Successfully sent email to [{}]", to);

        } catch (Exception e) {
            log.error("Failed to send email to [{}]. Reason: {}", to, e.getMessage(), e);
        }
    }
}
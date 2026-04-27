package com.app.aml.feature.casemanagement.service;

import com.app.aml.enums.CaseStatus;
import com.app.aml.feature.casemanagement.dto.caseAuditTrail.CaseAuditTrailResponseDto;
import com.app.aml.feature.casemanagement.dto.request.CaseNoteRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseAlertLink;
import com.app.aml.feature.casemanagement.entity.CaseAuditTrail;
import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseAlertLinkRepository;
import com.app.aml.feature.casemanagement.repository.CaseAuditTrailRepository;
import com.app.aml.feature.casemanagement.repository.CaseNoteRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.alert.mapper.AlertMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseInvestigationServiceImpl implements CaseInvestigationService {

    private final CaseRecordRepository caseRepo;
    private final CaseNoteRepository noteRepo;
    private final CaseAuditTrailRepository trailRepo;
    private final CaseAlertLinkRepository linkRepo;
    private final AlertMapper alertMapper;
    private final TemplateEngine templateEngine;

    @Override
    @Transactional
    public void openCase(UUID caseId, UUID actorId, String ip) {
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        if (caseRecord.getStatus() == CaseStatus.OPEN) {
            caseRecord.setStatus(CaseStatus.IN_PROGRESS);
            caseRecord.setLastActivityAt(Instant.now());
            caseRepo.save(caseRecord);

            CaseAuditTrail trail = new CaseAuditTrail();
            trail.setCaseRecord(caseRecord);
            trail.setActorId(actorId);
            trail.setIpAddress(ip);
            trail.setEventType("ASSIGNED");
            trail.setEventMetadata("{\"from\": \"OPEN\", \"to\": \"IN_PROGRESS\"}");
            trailRepo.save(trail);
        }
    }

    @Override
    @Transactional
    public void addNote(UUID caseId, CaseNoteRequestDto dto, UUID authoredBy, String ip) {
        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        CaseNote note = new CaseNote();
        note.setCaseRecord(caseRecord);
        note.setAuthoredBy(authoredBy);
        note.setNoteType(dto.getNoteType());
        note.setNoteContent(dto.getNoteContent());
        noteRepo.save(note);

        caseRecord.setHasInvestigationNote(true);
        caseRecord.setLastActivityAt(Instant.now());
        caseRepo.save(caseRecord);

        CaseAuditTrail trail = new CaseAuditTrail();
        trail.setCaseRecord(caseRecord);
        trail.setActorId(authoredBy);
        trail.setIpAddress(ip);
        trail.setEventType("NOTE_ADDED");
        trail.setEventMetadata("{\"noteType\": \"" + dto.getNoteType().name() + "\"}");
        trailRepo.save(trail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseAuditTrailResponseDto> getCaseAuditTrail(UUID caseId) {
        return trailRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(caseId).stream()
                .map(trail -> CaseAuditTrailResponseDto.builder()
                        .id(trail.getId())
                        .actorId(trail.getActorId())
                        .eventType(trail.getEventType())
                        .eventMetadata(trail.getEventMetadata())
                        .ipAddress(trail.getIpAddress())
                        .sysCreatedAt(trail.getSysCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailAsPdf(UUID caseId) {

        CaseRecord caseRecord = caseRepo.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));

        List<CaseAuditTrail> trails = trailRepo.findByCaseRecordIdOrderBySysCreatedAtDesc(caseId);

        Context context = new Context();
        context.setVariable("caseReference", caseRecord.getCaseReference());
        context.setVariable("status", caseRecord.getStatus().name());
        context.setVariable("trails", trails);

        String formattedDate = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
        context.setVariable("generatedOn", formattedDate);

        String htmlContent = templateEngine.process("pdf/case-audit-trail", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (com.lowagie.text.DocumentException | java.io.IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDto> getAlertsForCase(UUID caseId) {
        if (!caseRepo.existsById(caseId)) {
            throw new EntityNotFoundException("Case not found");
        }

        List<CaseAlertLink> links = linkRepo.findByCaseRecordIdWithAlerts(caseId);

        return links.stream()
                .map(CaseAlertLink::getAlert)
                .map(alertMapper::toResponseDto)
                .toList();
    }
}
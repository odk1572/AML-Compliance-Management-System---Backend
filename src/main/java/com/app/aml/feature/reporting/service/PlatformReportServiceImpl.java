package com.app.aml.feature.reporting.service;

import com.app.aml.feature.reporting.dtos.PlatformReportDtos.*;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import com.app.aml.annotation.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformReportServiceImpl implements PlatformReportService {

    private final TenantRepository tenantRepo;
    private final DataSource dataSource;

    @Override
    @AuditAction(category = "REPORTING", action = "GENERATE_GLOBAL_SAR_SUMMARY", entityType = "REPORT")
    public List<SarSummaryDto> getSarSummary(LocalDate from, LocalDate to) {
        List<SarSummaryDto> globalResults = new ArrayList<>();
        List<Tenant> tenants = tenantRepo.findAllActive();

        for (Tenant tenant : tenants) {
            executeInTenantSchema(tenant.getSchemaName(), (jdbcTemplate) -> {
                String sql = "SELECT COUNT(*) as cnt FROM str_filings WHERE sys_created_at BETWEEN ? AND ?";
                Long count = jdbcTemplate.queryForObject(sql, Long.class, from.atStartOfDay(), to.atTime(23, 59, 59));

                globalResults.add(new SarSummaryDto(tenant.getInstitutionName(), count, null));
            });
        }
        return globalResults;
    }



    private void executeInTenantSchema(String schemaName, java.util.function.Consumer<JdbcTemplate> action) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET search_path TO " + schemaName + ", public");

            SingleConnectionDataSource singleConnectionDs = new SingleConnectionDataSource(connection, true);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(singleConnectionDs);

            action.accept(jdbcTemplate);

            statement.execute("SET search_path TO public");

        } catch (Exception e) {
            log.error("Failed to execute query in schema: {}", schemaName, e);
            throw new RuntimeException("Cross-tenant reporting failed for schema: " + schemaName, e);
        }
    }
}
package com.app.aml.multitenency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantSchemaResolver tenantSchemaResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();


        if (path.startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = extractTenantId(request);

        try {
            if (tenantId != null) {
                String schemaName = tenantSchemaResolver.resolveSchema(tenantId);

                TenantContext.setTenantId(tenantId);
                TenantContext.setSchemaName(schemaName);

                log.trace("TenantContext bound: ID={}, Schema={}", tenantId, schemaName);
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
    private String extractTenantId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getDetails() instanceof Map claims) {
            return (String) claims.get("tenantId");
        }

        Object tenantAttribute = request.getAttribute("tenantId");
        if (tenantAttribute instanceof String tId) {
            return tId;
        }

        return request.getHeader("X-Tenant-ID");
    }
}
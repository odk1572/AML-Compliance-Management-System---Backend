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

/**
 * Filter that executes once per HTTP request to extract the Tenant ID
 * from the authenticated user and bind it to the thread-local TenantContext.
 */
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

        // 1. Extract the tenant ID from the JWT claims (via Security Context)
        String tenantId = extractTenantId(request);

        try {
            if (tenantId != null) {
                // Optional fail-fast: Validate the tenant actually has a schema mapped.
                // If this throws an exception, it prevents the request from proceeding further.
                tenantSchemaResolver.resolveSchema(tenantId);

                // Bind the ID to the current thread
                TenantContext.setTenantId(tenantId);
                log.trace("TenantContext bound for tenantId: {}", tenantId);
            }

            // 2. Continue down the filter chain (Controller -> Service -> Repo -> DataSource)
            filterChain.doFilter(request, response);

        } finally {
            // 3. CRITICAL: Clear the context
            // Spring uses a thread pool (like Tomcat's NIO threads).
            // If we don't clear this, the next request processed by this thread
            // will inherit the previous bank's Tenant ID, causing a massive data breach.
            TenantContext.clear();
            log.trace("TenantContext cleared for thread: {}", Thread.currentThread().getName());
        }
    }

    /**
     * Helper method to extract the tenantId.
     * Since the JwtAuthenticationFilter runs BEFORE this filter, the JWT claims
     * should already be parsed and placed into the SecurityContext or request attributes.
     */
    private String extractTenantId(HttpServletRequest request) {
        // Primary approach: Extract from the Principal configured by your JwtAuthFilter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getDetails() instanceof Map claims) {
            // If your JwtAuthFilter puts the raw claims into the 'details' object
            return (String) claims.get("tenantId");
        }

        // Fallback approach: Extract from a custom request attribute
        Object tenantAttribute = request.getAttribute("tenantId");
        if (tenantAttribute instanceof String tId) {
            return tId;
        }

        // Developer/System approach: Check for a direct HTTP header (useful for Super Admins)
        return request.getHeader("X-Tenant-ID");
    }
}
package com.app.aml.security.rbac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Intercepts requests to verify if the authenticated user's Role possesses
 * the necessary Permissions for the requested action based on Path and HTTP Method.
 */
@Slf4j
@Component
public class RbacAuthorizationFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // The registry of URL paths to required permissions.
    // Key Format: "HTTP_METHOD:/path/pattern/**" (Use "*" for any method)
    private static final Map<String, Permission> PATH_PERMISSION_MAP = new LinkedHashMap<>();

    static {
        // ====================================================================
        // ORDER MATTERS: Most Specific Rules First, Broader Rules Last
        // ====================================================================

        // 1. SYSTEM ADMIN (Platform Level)
        PATH_PERMISSION_MAP.put("*:/api/v1/platform/rules/**", Permission.GLOBAL_RULE_MANAGE);
        PATH_PERMISSION_MAP.put("*:/api/v1/platform/tenants/**", Permission.TENANT_ONBOARD);
        PATH_PERMISSION_MAP.put("*:/api/v1/platform/reports/cross-tenant/**", Permission.REPORT_CROSS_TENANT);
        PATH_PERMISSION_MAP.put("*:/api/v1/platform/reports/system/**", Permission.REPORT_SYSTEM_WIDE);
        PATH_PERMISSION_MAP.put("*:/api/v1/platform/**", Permission.TENANT_ONBOARD); // Fallback for other platform actions

        // 2. BANK ADMIN (Tenant Level)
        PATH_PERMISSION_MAP.put("*:/api/v1/admin/users/**", Permission.BANK_USER_MANAGE);
        PATH_PERMISSION_MAP.put("*:/api/v1/admin/reports/**", Permission.REPORT_INSTITUTIONAL);

        // 3. DATA INGESTION
        PATH_PERMISSION_MAP.put("*:/api/v1/batches/**", Permission.BATCH_UPLOAD);

        // 4. SUSPICIOUS TRANSACTION REPORTS (STRs)
        PATH_PERMISSION_MAP.put("*:/api/v1/filings/**", Permission.STR_FILE);

        // 5. CASE MANAGEMENT & INVESTIGATION
        PATH_PERMISSION_MAP.put("PUT:/api/v1/cases/*/assign", Permission.CASE_ASSIGN);

        // Notes / Audit Trail
        PATH_PERMISSION_MAP.put("POST:/api/v1/cases/*/notes", Permission.CASE_NOTE_WRITE);
        PATH_PERMISSION_MAP.put("GET:/api/v1/cases/*/notes", Permission.CASE_NOTE_READ);
        PATH_PERMISSION_MAP.put("GET:/api/v1/cases/*/audit", Permission.CASE_NOTE_READ);

        // General Case Investigation
        PATH_PERMISSION_MAP.put("PUT:/api/v1/cases/**", Permission.CASE_INVESTIGATE);
        PATH_PERMISSION_MAP.put("GET:/api/v1/cases/**", Permission.CASE_INVESTIGATE);

        // 6. ALERTS
        PATH_PERMISSION_MAP.put("PUT:/api/v1/alerts/*/process", Permission.CASE_INVESTIGATE); // Matrix puts investigation on COs
        PATH_PERMISSION_MAP.put("GET:/api/v1/alerts/**", Permission.ALERT_READ);

        // 7. CORE DATA CONTEXT (Read Only Historical)
        PATH_PERMISSION_MAP.put("GET:/api/v1/transactions/**", Permission.TRANSACTION_READ);
        PATH_PERMISSION_MAP.put("GET:/api/v1/customers/**", Permission.TRANSACTION_READ); // Grouped with Historical Review context
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Skip authorization check if the user isn't authenticated yet
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Determine Required Permission for this Request
        Permission requiredPermission = resolveRequiredPermission(request.getMethod(), request.getRequestURI());

        // 3. If the endpoint is protected by a permission, enforce RBAC
        if (requiredPermission != null) {
            Role userRole = extractRole(auth);

            if (userRole == null || !RolePermissionRegistry.hasPermission(userRole, requiredPermission)) {
                log.warn("RBAC Denied: User Role [{}] attempted to access [{}] {} without {} permission.",
                        userRole, request.getMethod(), request.getRequestURI(), requiredPermission);

                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions to perform this action.");
                return; // Halt the chain
            }
        }

        // 4. Access Granted (or Endpoint didn't require specific permission)
        filterChain.doFilter(request, response);
    }

    /**
     * Iterates through the LinkedHashMap. Returns the first matching Permission.
     */
    private Permission resolveRequiredPermission(String httpMethod, String requestUri) {
        for (Map.Entry<String, Permission> entry : PATH_PERMISSION_MAP.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String mappedMethod = parts[0];
            String mappedPattern = parts[1];

            // Does the Method match? ("*" means any method)
            boolean methodMatches = "*".equals(mappedMethod) || mappedMethod.equalsIgnoreCase(httpMethod);

            // Does the URI match the Ant Pattern?
            if (methodMatches && pathMatcher.match(mappedPattern, requestUri)) {
                log.debug("Path {} matched to permission {}", requestUri, entry.getValue());
                return entry.getValue();
            }
        }
        return null; // No specific permission required for this path
    }

    /**
     * Extracts the Role enum from the Spring Security GrantedAuthorities.
     */
    private Role extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .filter(roleStr -> {
                    try {
                        Role.valueOf(roleStr);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false; // Safely ignore invalid roles
                    }
                })
                .map(Role::valueOf)
                .findFirst()
                .orElse(null);
    }
}
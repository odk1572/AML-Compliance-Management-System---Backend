package com.app.aml.utils;


import com.app.aml.security.userDetails.PlatformUserDetails;
import com.app.aml.security.userDetails.TenantUserDetails;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@UtilityClass
public class SecurityUtils {

    public static UUID getCurrentUserId() {
        return getCurrentUserIdOptional()
                .orElseThrow(() -> new RuntimeException("Unauthorized: No user ID found in security context."));
    }

    public static Optional<UUID> getCurrentUserIdOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        try {
            String principalId = auth.getName();
            return Optional.of(UUID.fromString(principalId));
        } catch (Exception e) {
            log.error("Failed to parse User ID from Security Context", e);
            return Optional.empty();
        }
    }

    public static String getCurrentUserEmail() {
        return getCurrentUserEmailOptional()
                .orElseThrow(() -> new RuntimeException("Unauthorized: No email found in security context."));
    }

    public static Optional<String> getCurrentUserEmailOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof PlatformUserDetails details) {
            return Optional.ofNullable(details.getPlatformUser().getEmail());
        }

        if (principal instanceof TenantUserDetails details) {
            return Optional.ofNullable(details.getTenantUser().getEmail());
        }

        return Optional.ofNullable(auth.getName());
    }

    public static String getRemoteIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "0.0.0.0";

        HttpServletRequest request = attributes.getRequest();

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }


    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser");
    }
}
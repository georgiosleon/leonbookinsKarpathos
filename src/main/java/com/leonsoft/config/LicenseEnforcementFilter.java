package com.leonsoft.config;

import com.leonsoft.services.LicenseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LicenseEnforcementFilter extends OncePerRequestFilter {

    private static final Set<String> ALWAYS_ALLOWED_PATHS = Set.of(
            "/login", "/logout", "/error", "/favicon.ico"
    );
    private static final Set<String> ALWAYS_ALLOWED_PREFIXES = Set.of(
            "/css/", "/js/", "/react-assets/"
    );
    private static final Set<String> LICENSE_API_PREFIXES = Set.of(
            "/api/v1/license", "/api/v1/customers"
    );

    private final LicenseService licenseService;

    public LicenseEnforcementFilter(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (licenseService.isActivated() || isAllowedWhenUnlicensed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = requestPath(request);
        if (path.startsWith("/api/")) {
            writeLicenseError(response, request);
            return;
        }

        // Allow SPA shells/static navigation so frontend can render activation/block screen.
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWhenUnlicensed(HttpServletRequest request) {
        String path = requestPath(request);
        if (ALWAYS_ALLOWED_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : ALWAYS_ALLOWED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        if (path.equals("/api/v1/csrf") || path.equals("/api/v1/users/me") || path.equals("/api/v1/license/status")) {
            return true;
        }
        for (String prefix : LICENSE_API_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return isAdmin();
            }
        }
        // Allow SPA entry pages; frontend decides what to show based on role/license status.
        if (path.equals("/") || path.equals("/index.html") || path.equals("/rooms.html") || path.equals("/cars.html")) {
            return true;
        }
        return false;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void writeLicenseError(HttpServletResponse response, HttpServletRequest request) throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        response.setStatus(status.value());
        response.setContentType("application/json");
        String path = requestPath(request).replace("\\", "\\\\").replace("\"", "\\\"");
        String message = "License activation required. Please contact an administrator.";
        String body = "{\"timestamp\":\""
                + Instant.now()
                + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message
                + "\",\"path\":\"" + path + "\"}";
        response.getWriter().write(body);
    }

    private static String requestPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return path == null ? "" : path;
    }
}

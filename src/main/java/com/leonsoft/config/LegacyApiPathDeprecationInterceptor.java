package com.leonsoft.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LegacyApiPathDeprecationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (isLegacyPath(path)) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Warning", "299 - \"Deprecated API path; use /api/v1/...\"");
        }
        return true;
    }

    static boolean isLegacyPath(String path) {
        if (path == null) {
            return false;
        }
        return "/csrf".equals(path) || path.startsWith("/booking");
    }
}

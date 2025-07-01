package com.leonsoft.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {



    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // Log request details
        log.info("Received request: {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, @NonNull Object handler, Exception ex) {
        // Log response details
        log.info("Sent response: {}  and exception  ",
              request.getMethod() +"    "+ request.getRequestURI() +  " with status  "+  response.getStatus(), ex);
    }
}
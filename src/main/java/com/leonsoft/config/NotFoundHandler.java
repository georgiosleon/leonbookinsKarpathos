package com.leonsoft.config;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
@Slf4j
public class NotFoundHandler {
    private final Resource indexPage;

    public NotFoundHandler(
          ResourceLoader resourceLoader,
          @Value("${spa.default-file:classpath:/public/index.html}") String defaultFile) {
        this.indexPage = resourceLoader.getResource(defaultFile);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> renderDefaultPage(
          NoHandlerFoundException exception, HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (!"GET".equalsIgnoreCase(exception.getHttpMethod())
              || !"/".equals(request.getRequestURI())
              || accept == null
              || !accept.toLowerCase(java.util.Locale.ROOT).contains(MediaType.TEXT_HTML_VALUE)) {
            return ResponseEntity.notFound().build();
        }

        try (var inputStream = indexPage.getInputStream()) {
            String body = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                  .contentType(MediaType.TEXT_HTML)
                  .body(body);
        } catch (IOException exceptionReadingPage) {
            log.error("Failed to load SPA entry page", exceptionReadingPage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body("There was an error completing the action.");
        }
    }
}

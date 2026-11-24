package com.leonsoft.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@ControllerAdvice
public class CustomErrorHandler {

    @ExceptionHandler({
          ConstraintViolationException.class,
          org.hibernate.exception.ConstraintViolationException.class,
          HttpMessageNotReadableException.class,
          MethodArgumentNotValidException.class,
          MethodArgumentTypeMismatchException.class,
          MissingServletRequestParameterException.class,
          IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception, HttpServletRequest request, java.util.Locale locale) {
        String message = "el".equals(locale.getLanguage())
              ? "Τα δεδομένα του αιτήματος δεν είναι έγκυρα."
              : "Invalid request.";
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatusException(
          ResponseStatusException exception, HttpServletRequest request, java.util.Locale locale) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = exception.getReason();
        if ("Booking not found.".equals(message) && "el".equals(locale.getLanguage())) {
            message = "Δεν βρέθηκε η κράτηση.";
        }
        if (message == null || message.isBlank()) {
            message = status.is4xxClientError() ? "Request failed." : "Unexpected server error.";
        }
        return build(status, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, status.name(), message, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        String path = request == null ? "" : request.getRequestURI();
        return ResponseEntity.status(status)
              .body(new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), code, message, path));
    }

    public record ApiError(String timestamp, int status, String error, String code, String message, String path) {
    }
}

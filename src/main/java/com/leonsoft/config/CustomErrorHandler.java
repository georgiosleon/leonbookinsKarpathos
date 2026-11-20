package com.leonsoft.config;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomErrorHandler {

    @ExceptionHandler({
          ConstraintViolationException.class,
          HttpMessageNotReadableException.class,
          MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(java.util.Locale locale) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(new ErrorResponse("el".equals(locale.getLanguage())
                    ? "Τα δεδομένα του αιτήματος δεν είναι έγκυρα."
                    : "Invalid request."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatusException(
          ResponseStatusException exception, java.util.Locale locale) {
        String message = exception.getReason();
        if ("Booking not found.".equals(message) && "el".equals(locale.getLanguage())) {
            message = "Δεν βρέθηκε η κράτηση.";
        }
        return ResponseEntity.status(exception.getStatusCode())
              .body(new ErrorResponse(message == null ? "Request failed." : message));
    }

    public record ErrorResponse(String message) {
    }
}

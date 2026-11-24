package com.leonsoft.api;

import com.leonsoft.services.ApplicationSupportService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationSupportController {
    private final ApplicationSupportService supportService;

    public ApplicationSupportController(ApplicationSupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/api/v1/app-info")
    ApplicationSupportService.AppInfo appInfo() {
        return supportService.appInfo();
    }

    @GetMapping("/api/v1/diagnostics")
    ResponseEntity<byte[]> diagnostics() {
        byte[] report = supportService.diagnostics().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
              .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leon-bookings-diagnostics.txt")
              .body(report);
    }

    @GetMapping("/api/v1/support-bundle")
    ResponseEntity<byte[]> supportBundle() {
        return ResponseEntity.ok()
              .contentType(MediaType.parseMediaType("application/zip"))
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leon-bookings-support-bundle.zip")
              .body(supportService.supportBundle());
    }
}

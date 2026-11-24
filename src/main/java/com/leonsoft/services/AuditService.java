package com.leonsoft.services;

import com.leonsoft.models.AuditLog;
import com.leonsoft.repositories.AuditLogRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUserProvider currentUserProvider) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void record(String action, String entityType, String entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setCreatedAt(LocalDateTime.now());
            entry.setActorUsername(currentUserProvider.getCurrentUser().map(u -> u.getUsername()).orElse("system"));
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log action={} entityType={} entityId={}", action, entityType, entityId, e);
        }
    }
}

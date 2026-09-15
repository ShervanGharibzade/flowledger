package org.fl.flowledger.audit.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.audit.dto.AuditAction;
import org.fl.flowledger.audit.dto.AuditEntityType;
import org.fl.flowledger.audit.entity.AuditLog;
import org.fl.flowledger.audit.repository.AuditLogRepository;
import org.fl.flowledger.user.entity.User;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(
            User user,
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, Object> metadata,
            InetAddress ipAddress
    ) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setMetadata(metadata);
        auditLog.setIpAddress(ipAddress);

        auditLogRepository.save(auditLog);
    }
}

package com.mpfc.securebankingsystem.service;

import com.mpfc.securebankingsystem.entity.AuditLog;
import com.mpfc.securebankingsystem.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {
    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String username, String action, String referenceId, String details) {
        AuditLog l = new AuditLog();
        l.setUsername(username);
        l.setAction(action);
        l.setTimestamp(Instant.now());
        l.setReferenceId(referenceId);
        l.setDetails(details);
        repo.save(l);
    }
}

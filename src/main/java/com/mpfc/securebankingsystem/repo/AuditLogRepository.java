package com.mpfc.securebankingsystem.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mpfc.securebankingsystem.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}

package com.mpfc.securebankingsystem.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mpfc.securebankingsystem.entity.FileEncrypted;

public interface EncryptedFileRepository extends JpaRepository<FileEncrypted, Long> {
    Optional<FileEncrypted> findByChecksumSha256(String checksumSha256);
}

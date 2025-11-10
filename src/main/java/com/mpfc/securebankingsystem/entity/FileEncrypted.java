package com.mpfc.securebankingsystem.entity;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "files_encrypted")
public class FileEncrypted {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String uploader;
    private Instant uploadedAt;
    private String encryptionAlgo; // e.g., AES/GCM/NoPadding
    private String checksumSha256;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] iv;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] cipherData;
}

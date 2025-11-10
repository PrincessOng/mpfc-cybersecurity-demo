package com.mpfc.securebankingsystem.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    private final byte[] keyBytes;
    private SecretKeySpec keySpec;
    private final SecureRandom rng = new SecureRandom();

    public record CryptoResult(byte[] iv, byte[] cipher) {}

    public EncryptionService(@Value("${security.encryption.key}") String base64Key) {
        this.keyBytes = Base64.getDecoder().decode(base64Key);
    }

    @PostConstruct
    void init() {
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public CryptoResult encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[IV_LEN];
        rng.nextBytes(iv);
        Cipher c = Cipher.getInstance(TRANSFORMATION);
        c.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] cipher = c.doFinal(plaintext);
        return new CryptoResult(iv, cipher);
    }

    public byte[] decrypt(byte[] iv, byte[] cipher) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORMATION);
        c.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return c.doFinal(cipher);
    }
}

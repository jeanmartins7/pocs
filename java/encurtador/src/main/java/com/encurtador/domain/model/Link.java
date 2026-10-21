package com.encurtador.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public class Link {

    private final ShortCode code;
    private final TargetUrl targetUrl;
    private final Instant createdAtUtc;
    private final Instant expiresAtUtc;
    private long accessCount;
    private final String managementKeyHash;

    public Link(ShortCode code, TargetUrl targetUrl, Instant createdAtUtc, Instant expiresAtUtc, long accessCount, String managementKeyHash) {
        this.code = Objects.requireNonNull(code, "O código não pode ser nulo");
        this.targetUrl = Objects.requireNonNull(targetUrl, "A URL de destino não pode ser nula");
        this.createdAtUtc = Objects.requireNonNull(createdAtUtc, "A data de criação não pode ser nula");
        this.expiresAtUtc = expiresAtUtc;
        if (accessCount < 0) {
            throw new IllegalArgumentException("O contador de acessos não pode ser negativo.");
        }
        this.accessCount = accessCount;
        this.managementKeyHash = Objects.requireNonNull(managementKeyHash, "O hash da chave de gerenciamento não pode ser nulo");
    }

    public static Link create(ShortCode code, TargetUrl targetUrl, Instant expiresAtUtc, String rawManagementKey) {
        Instant now = Instant.now();
        if (expiresAtUtc != null && expiresAtUtc.isBefore(now)) {
            throw new IllegalArgumentException("A data de expiração não pode estar no passado.");
        }
        String keyHash = hashKey(rawManagementKey);
        return new Link(code, targetUrl, now, expiresAtUtc, 0L, keyHash);
    }

    public static String generateManagementKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String hashKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("A chave de gerenciamento não pode ser vazia.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256", e);
        }
    }

    public boolean matchesManagementKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }
        String computedHash = hashKey(rawKey);
        return MessageDigest.isEqual(this.managementKeyHash.getBytes(StandardCharsets.UTF_8),
                computedHash.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isExpired(Instant instant) {
        if (expiresAtUtc == null) {
            return false;
        }
        return instant.isAfter(expiresAtUtc);
    }

    public void updateAccessCount(long newCount) {
        if (newCount < this.accessCount) {
            throw new IllegalArgumentException("O contador de acessos não pode diminuir.");
        }
        this.accessCount = newCount;
    }

    public ShortCode getCode() {
        return code;
    }

    public TargetUrl getTargetUrl() {
        return targetUrl;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }

    public Instant getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public String getManagementKeyHash() {
        return managementKeyHash;
    }
}

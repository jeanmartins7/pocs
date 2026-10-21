package com.encurtador.domain.port.in;

import java.time.Instant;

public record LinkStats(
        String code,
        String targetUrl,
        Instant createdAtUtc,
        Instant expiresAtUtc,
        long accessCount
) {}

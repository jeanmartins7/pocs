package com.encurtador.infrastructure.adapter.in.web.dto;

import java.time.Instant;

public record LinkStatsResponse(
        String code,
        String targetUrl,
        Instant createdAtUtc,
        Instant expiresAtUtc,
        long accessCount
) {}

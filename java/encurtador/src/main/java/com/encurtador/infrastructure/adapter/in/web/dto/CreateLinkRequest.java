package com.encurtador.infrastructure.adapter.in.web.dto;

import java.time.Instant;

public record CreateLinkRequest(
        String targetUrl,
        String code,
        Instant expiresAtUtc
) {}

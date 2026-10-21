package com.encurtador.domain.port.in;

import java.time.Instant;

public record CreateLinkCommand(
        String targetUrl,
        String customCode,
        Instant expiresAtUtc,
        String baseUrl
) {}

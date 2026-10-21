package com.encurtador.domain.port.in;

public record CreatedLink(
        String shortUrl,
        String code,
        String managementKey
) {}

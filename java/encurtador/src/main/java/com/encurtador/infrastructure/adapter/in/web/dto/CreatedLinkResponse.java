package com.encurtador.infrastructure.adapter.in.web.dto;

public record CreatedLinkResponse(
        String shortUrl,
        String code,
        String managementKey
) {}

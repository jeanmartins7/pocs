package com.encurtador.application.service;

import com.encurtador.domain.exception.CodeAlreadyExistsException;
import com.encurtador.domain.exception.InvalidManagementKeyException;
import com.encurtador.domain.exception.LinkExpiredException;
import com.encurtador.domain.exception.LinkNotFoundException;
import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;
import com.encurtador.domain.model.TargetUrl;
import com.encurtador.domain.port.in.CreateLinkCommand;
import com.encurtador.domain.port.in.CreateLinkUseCase;
import com.encurtador.domain.port.in.CreatedLink;
import com.encurtador.domain.port.in.GetLinkStatsUseCase;
import com.encurtador.domain.port.in.LinkStats;
import com.encurtador.domain.port.in.RedirectLinkUseCase;
import com.encurtador.domain.port.out.LinkCachePort;
import com.encurtador.domain.port.out.LinkRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LinkService implements CreateLinkUseCase, RedirectLinkUseCase, GetLinkStatsUseCase {

    private static final int MAX_RETRIES = 10;
    private final LinkRepositoryPort repository;
    private final LinkCachePort cache;

    public LinkService(LinkRepositoryPort repository, LinkCachePort cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public CreatedLink create(CreateLinkCommand command) {
        TargetUrl targetUrl = TargetUrl.of(command.targetUrl());

        if (command.customCode() != null && !command.customCode().isBlank()) {
            ShortCode customCode = ShortCode.custom(command.customCode());
            String key = Link.generateManagementKey();
            Link link = Link.create(customCode, targetUrl, command.expiresAtUtc(), key);

            boolean saved = repository.saveIfAbsent(link);
            if (!saved) {
                throw new CodeAlreadyExistsException("O código " + customCode.value() + " já está em uso.");
            }
            cache.put(link);
            return new CreatedLink(buildShortUrl(command.baseUrl(), customCode), customCode.value(), key);
        }

        for (int i = 0; i < MAX_RETRIES; i++) {
            ShortCode autoCode = ShortCode.generateRandom();
            String key = Link.generateManagementKey();
            Link link = Link.create(autoCode, targetUrl, command.expiresAtUtc(), key);

            if (repository.saveIfAbsent(link)) {
                cache.put(link);
                return new CreatedLink(buildShortUrl(command.baseUrl(), autoCode), autoCode.value(), key);
            }
        }

        throw new IllegalStateException("Não foi possível gerar um código único após múltiplas tentativas.");
    }

    @Override
    public String getTargetUrlAndIncrementCount(String codeInput) {
        ShortCode code = ShortCode.fromInput(codeInput);

        Link link = cache.get(code).orElse(null);
        if (link == null) {
            link = repository.findByCode(code)
                    .orElseThrow(() -> new LinkNotFoundException("Link não encontrado para o código: " + code.value()));
            cache.put(link);
        }

        if (link.isExpired(Instant.now())) {
            cache.evict(code);
            throw new LinkExpiredException("O link está expirado.");
        }

        repository.incrementAccessCount(code);
        return link.getTargetUrl().value();
    }

    @Override
    public LinkStats getStats(String codeInput, String managementKey) {
        ShortCode code = ShortCode.fromInput(codeInput);
        Link link = repository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException("Link não encontrado para o código: " + code.value()));

        if (!link.matchesManagementKey(managementKey)) {
            throw new InvalidManagementKeyException("Chave de gerenciamento inválida.");
        }

        return new LinkStats(
                link.getCode().value(),
                link.getTargetUrl().value(),
                link.getCreatedAtUtc(),
                link.getExpiresAtUtc(),
                link.getAccessCount()
        );
    }

    private String buildShortUrl(String baseUrl, ShortCode code) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "/" + code.value();
        }
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return cleanBase + "/" + code.value();
    }
}

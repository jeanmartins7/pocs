package com.encurtador.application.service;

import com.encurtador.domain.exception.CodeAlreadyExistsException;
import com.encurtador.domain.exception.InvalidManagementKeyException;
import com.encurtador.domain.exception.LinkExpiredException;
import com.encurtador.domain.exception.LinkNotFoundException;
import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;
import com.encurtador.domain.model.TargetUrl;
import com.encurtador.domain.port.in.CreateLinkCommand;
import com.encurtador.domain.port.in.CreatedLink;
import com.encurtador.domain.port.in.LinkStats;
import com.encurtador.domain.port.out.LinkCachePort;
import com.encurtador.domain.port.out.LinkRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepositoryPort repository;

    @Mock
    private LinkCachePort cache;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(repository, cache);
    }

    @Test
    @DisplayName("Deve criar link com código automático com sucesso (T-05)")
    void shouldCreateLinkWithAutomaticCode() {
        when(repository.saveIfAbsent(any(Link.class))).thenReturn(true);

        CreateLinkCommand command = new CreateLinkCommand(
                "https://example.com/target",
                null,
                null,
                "http://localhost:8080"
        );

        CreatedLink created = linkService.create(command);

        assertThat(created.code()).hasSize(7);
        assertThat(created.shortUrl()).isEqualTo("http://localhost:8080/" + created.code());
        assertThat(created.managementKey()).isNotBlank();
        verify(repository, times(1)).saveIfAbsent(any(Link.class));
    }

    @Test
    @DisplayName("Deve tentar gerar outro código automático quando houver colisão (RN-06)")
    void shouldRetryGeneratingCodeOnCollision() {
        when(repository.saveIfAbsent(any(Link.class)))
                .thenReturn(false)
                .thenReturn(true);

        CreateLinkCommand command = new CreateLinkCommand(
                "https://example.com/target",
                null,
                null,
                "http://localhost:8080"
        );

        CreatedLink created = linkService.create(command);

        assertThat(created.code()).hasSize(7);
        verify(repository, times(2)).saveIfAbsent(any(Link.class));
    }

    @Test
    @DisplayName("Deve criar link com código personalizado válido (T-06)")
    void shouldCreateLinkWithCustomCode() {
        when(repository.saveIfAbsent(any(Link.class))).thenReturn(true);

        CreateLinkCommand command = new CreateLinkCommand(
                "https://example.com/target",
                "MYCUSTOMCODE",
                null,
                "http://localhost:8080"
        );

        CreatedLink created = linkService.create(command);

        assertThat(created.code()).isEqualTo("MYCUSTOMCODE");
        assertThat(created.shortUrl()).isEqualTo("http://localhost:8080/MYCUSTOMCODE");
    }

    @Test
    @DisplayName("Deve lançar CodeAlreadyExistsException para código próprio duplicado (T-06)")
    void shouldThrowConflictForDuplicateCustomCode() {
        when(repository.saveIfAbsent(any(Link.class))).thenReturn(false);

        CreateLinkCommand command = new CreateLinkCommand(
                "https://example.com/target",
                "TAKENCODE",
                null,
                "http://localhost:8080"
        );

        assertThatThrownBy(() -> linkService.create(command))
                .isInstanceOf(CodeAlreadyExistsException.class)
                .hasMessageContaining("O código TAKENCODE já está em uso");
    }

    @Test
    @DisplayName("Deve redirecionar e incrementar contador atomicamente (T-07, T-08)")
    void shouldRedirectAndIncrementCount() {
        ShortCode code = ShortCode.fromInput("ABC1234");
        TargetUrl url = TargetUrl.of("https://example.com/target");
        Link link = Link.create(code, url, null, Link.generateManagementKey());

        when(cache.get(code)).thenReturn(Optional.empty());
        when(repository.findByCode(code)).thenReturn(Optional.of(link));
        when(repository.incrementAccessCount(code)).thenReturn(1L);

        String targetUrl = linkService.getTargetUrlAndIncrementCount("abc1234");

        assertThat(targetUrl).isEqualTo("https://example.com/target");
        verify(repository).incrementAccessCount(code);
        verify(cache).put(link);
    }

    @Test
    @DisplayName("Deve lançar LinkNotFoundException quando código não existir (T-07)")
    void shouldThrowNotFoundWhenCodeDoesNotExist() {
        ShortCode code = ShortCode.fromInput("UNKNOWN");
        when(cache.get(code)).thenReturn(Optional.empty());
        when(repository.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.getTargetUrlAndIncrementCount("UNKNOWN"))
                .isInstanceOf(LinkNotFoundException.class)
                .hasMessageContaining("Link não encontrado");

        verify(repository, never()).incrementAccessCount(any());
    }

    @Test
    @DisplayName("Deve lançar LinkExpiredException e não incrementar contador quando link estiver expirado (T-07, RN-03)")
    void shouldThrowExpiredAndNotIncrementCount() {
        ShortCode code = ShortCode.fromInput("EXPIRED1");
        TargetUrl url = TargetUrl.of("https://example.com/target");
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        Link expiredLink = new Link(code, url, past.minus(1, ChronoUnit.DAYS), past, 5, "hash");

        when(cache.get(code)).thenReturn(Optional.empty());
        when(repository.findByCode(code)).thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> linkService.getTargetUrlAndIncrementCount("EXPIRED1"))
                .isInstanceOf(LinkExpiredException.class)
                .hasMessageContaining("O link está expirado");

        verify(repository, never()).incrementAccessCount(any());
    }

    @Test
    @DisplayName("Deve retornar estatísticas do link com chave correta (T-09)")
    void shouldReturnStatsWithValidKey() {
        ShortCode code = ShortCode.fromInput("STATS12");
        TargetUrl url = TargetUrl.of("https://example.com/target");
        String key = Link.generateManagementKey();
        Link link = Link.create(code, url, null, key);
        link.updateAccessCount(42);

        when(repository.findByCode(code)).thenReturn(Optional.of(link));

        LinkStats stats = linkService.getStats("STATS12", key);

        assertThat(stats.code()).isEqualTo("STATS12");
        assertThat(stats.targetUrl()).isEqualTo("https://example.com/target");
        assertThat(stats.accessCount()).isEqualTo(42);
    }

    @Test
    @DisplayName("Deve lançar InvalidManagementKeyException quando chave for inválida (T-09)")
    void shouldThrowUnauthorizedForInvalidKey() {
        ShortCode code = ShortCode.fromInput("STATS12");
        TargetUrl url = TargetUrl.of("https://example.com/target");
        Link link = Link.create(code, url, null, Link.generateManagementKey());

        when(repository.findByCode(code)).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> linkService.getStats("STATS12", "wrong-key"))
                .isInstanceOf(InvalidManagementKeyException.class)
                .hasMessageContaining("Chave de gerenciamento inválida");
    }
}

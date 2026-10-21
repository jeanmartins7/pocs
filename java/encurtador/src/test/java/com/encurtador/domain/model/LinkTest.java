package com.encurtador.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkTest {

    @Test
    @DisplayName("Deve criar um link válido sem data de expiração")
    void shouldCreateValidLinkWithoutExpiration() {
        ShortCode code = ShortCode.fromInput("ABC1234");
        TargetUrl url = TargetUrl.of("https://example.com");
        String key = Link.generateManagementKey();
        Link link = Link.create(code, url, null, key);

        assertThat(link.getCode()).isEqualTo(code);
        assertThat(link.getTargetUrl()).isEqualTo(url);
        assertThat(link.getExpiresAtUtc()).isNull();
        assertThat(link.getAccessCount()).isZero();
        assertThat(link.isExpired(Instant.now())).isFalse();
        assertThat(link.matchesManagementKey(key)).isTrue();
        assertThat(link.matchesManagementKey("wrong-key")).isFalse();
    }

    @Test
    @DisplayName("Deve identificar corretamente quando o link está expirado")
    void shouldDetectExpiredLink() {
        ShortCode code = ShortCode.fromInput("ABC1234");
        TargetUrl url = TargetUrl.of("https://example.com");
        Instant now = Instant.now();
        Instant past = now.minus(1, ChronoUnit.HOURS);
        Instant future = now.plus(1, ChronoUnit.HOURS);

        Link activeLink = Link.create(code, url, future, Link.generateManagementKey());
        assertThat(activeLink.isExpired(now)).isFalse();

        Link expiredLink = new Link(code, url, now.minus(2, ChronoUnit.HOURS), past, 0, "hash");
        assertThat(expiredLink.isExpired(now)).isTrue();
    }

    @Test
    @DisplayName("Deve recusar criação de link com data de expiração no passado")
    void shouldRejectCreationWithPastExpiration() {
        ShortCode code = ShortCode.fromInput("ABC1234");
        TargetUrl url = TargetUrl.of("https://example.com");
        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> Link.create(code, url, past, Link.generateManagementKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A data de expiração não pode estar no passado");
    }

    @Test
    @DisplayName("Contador de acessos nunca deve diminuir")
    void counterShouldNeverDecrease() {
        ShortCode code = ShortCode.fromInput("ABC1234");
        TargetUrl url = TargetUrl.of("https://example.com");
        Link link = Link.create(code, url, null, Link.generateManagementKey());

        link.updateAccessCount(5);
        assertThat(link.getAccessCount()).isEqualTo(5);

        assertThatThrownBy(() -> link.updateAccessCount(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O contador de acessos não pode diminuir");
    }
}

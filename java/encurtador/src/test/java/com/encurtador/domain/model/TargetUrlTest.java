package com.encurtador.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.google.com",
            "http://example.com/path?query=1&b=2#hash",
            "https://sub.domain.org:8443/test"
    })
    @DisplayName("Deve aceitar URLs válidas públicas com http e https")
    void shouldAcceptValidPublicUrls(String validUrl) {
        TargetUrl targetUrl = TargetUrl.of(validUrl);
        assertThat(targetUrl.value()).isEqualTo(validUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com",
            "file:///path/to/file",
            "mailto:test@example.com",
            "javascript:alert(1)"
    })
    @DisplayName("Deve rejeitar protocolos diferentes de http e https")
    void shouldRejectNonHttpProtocols(String invalidProtocol) {
        assertThatThrownBy(() -> TargetUrl.of(invalidProtocol))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A URL deve utilizar o protocolo HTTP ou HTTPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost",
            "http://localhost:8080/path",
            "http://127.0.0.1",
            "http://127.0.0.2:3000",
            "http://10.0.0.1/admin",
            "https://10.255.255.255",
            "http://172.16.0.1",
            "http://172.31.255.255",
            "http://192.168.0.1",
            "http://192.168.1.100",
            "http://169.254.1.1",
            "http://[::1]"
    })
    @DisplayName("Deve rejeitar endereços locais ou de rede privada sem chamada externa")
    void shouldRejectLocalAndPrivateAddresses(String privateUrl) {
        assertThatThrownBy(() -> TargetUrl.of(privateUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endereços locais ou de redes privadas não são permitidos");
    }

    @Test
    @DisplayName("Deve rejeitar URL com mais de 2048 caracteres")
    void shouldRejectUrlExceeding2048Chars() {
        String base = "https://example.com/";
        String longPath = "a".repeat(2048);
        String tooLongUrl = base + longPath;

        assertThatThrownBy(() -> TargetUrl.of(tooLongUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A URL não pode exceder 2048 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not a url", "http://", "https://"})
    @DisplayName("Deve rejeitar formatos sintáticos inválidos")
    void shouldRejectInvalidSyntax(String invalidSyntax) {
        assertThatThrownBy(() -> TargetUrl.of(invalidSyntax))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

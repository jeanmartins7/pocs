package com.encurtador.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortCodeTest {

    @Test
    @DisplayName("Deve gerar código automático com exatamente 7 caracteres válidos")
    void shouldGenerateAutomaticCodeWith7ValidChars() {
        for (int i = 0; i < 50; i++) {
            ShortCode code = ShortCode.generateRandom();
            assertThat(code.value()).hasSize(7);
            assertThat(code.value()).matches("^[A-Z1-9]{7}$");
            assertThat(code.value()).doesNotContain("0");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCD", "CODE123", "MAXTWENTYCHARACTERS", "A1B2C3D4"})
    @DisplayName("Deve aceitar código personalizado válido entre 4 e 20 caracteres")
    void shouldAcceptValidCustomCode(String value) {
        ShortCode code = ShortCode.custom(value);
        assertThat(code.value()).isEqualTo(value.toUpperCase());
    }

    @Test
    @DisplayName("Deve converter código para maiúsculas ao normalizar para leitura")
    void shouldNormalizeCaseInsensitive() {
        ShortCode code = ShortCode.fromInput("abc123d");
        assertThat(code.value()).isEqualTo("ABC123D");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "TOO_LONG_CUSTOM_CODE_EXCEEDING_TWENTY_CHARS", "CODE 1", "COD@12", "CODE0", ""})
    @DisplayName("Deve rejeitar código personalizado inválido")
    void shouldRejectInvalidCustomCode(String invalidValue) {
        assertThatThrownBy(() -> ShortCode.custom(invalidValue))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

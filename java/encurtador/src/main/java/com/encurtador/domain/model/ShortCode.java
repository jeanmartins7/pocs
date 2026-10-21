package com.encurtador.domain.model;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

public record ShortCode(String value) {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789"; // or full A-Z without 0:
    // RF-02 & RN-05: "letras maiúsculas de A a Z e números de 1 a 9, sem o dígito 0"
    // D-03: "código do link gerado com alfabeto sem caracteres ambíguos"
    // Let's make sure it satisfies RF-02/RN-05: all A-Z and 1-9.
    private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
    private static final Pattern VALID_CODE_PATTERN = Pattern.compile("^[A-Z1-9]{4,20}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public ShortCode {
        Objects.requireNonNull(value, "O código não pode ser nulo");
        String normalized = value.trim().toUpperCase();
        if (!VALID_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("O código deve ter entre 4 e 20 caracteres compostos apenas por letras maiúsculas de A a Z e números de 1 a 9, sem o dígito 0 ou caracteres especiais.");
        }
        value = normalized;
    }

    public static ShortCode generateRandom() {
        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            int index = RANDOM.nextInt(ALLOWED_CHARACTERS.length());
            sb.append(ALLOWED_CHARACTERS.charAt(index));
        }
        return new ShortCode(sb.toString());
    }

    public static ShortCode custom(String customValue) {
        if (customValue == null || customValue.isBlank()) {
            throw new IllegalArgumentException("O código personalizado não pode ser vazio.");
        }
        return new ShortCode(customValue);
    }

    public static ShortCode fromInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("O código de busca não pode ser vazio.");
        }
        return new ShortCode(input);
    }

    @Override
    public String toString() {
        return value;
    }
}

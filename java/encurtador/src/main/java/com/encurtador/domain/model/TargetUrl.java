package com.encurtador.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Pattern;

public record TargetUrl(String value) {

    private static final int MAX_LENGTH = 2048;
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public TargetUrl {
        Objects.requireNonNull(value, "A URL não pode ser nula");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A URL não pode ser vazia.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("A URL não pode exceder 2048 caracteres.");
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Formato sintático de URL inválido: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("A URL deve utilizar o protocolo HTTP ou HTTPS.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("A URL deve possuir um host válido.");
        }

        validateNotPrivateOrLocal(host);
    }

    private static void validateNotPrivateOrLocal(String rawHost) {
        String host = rawHost.toLowerCase();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")) {
            throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
        }

        // IPv6 loopback and private checks
        if (host.equals("::1") || host.equals("0:0:0:0:0:0:0:1") || host.startsWith("fe80:") || host.startsWith("fc00:") || host.startsWith("fd00:")) {
            throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
        }

        // IPv4 checks
        var matcher = IPV4_PATTERN.matcher(host);
        if (matcher.matches()) {
            int octet1 = Integer.parseInt(matcher.group(1));
            int octet2 = Integer.parseInt(matcher.group(2));
            int octet3 = Integer.parseInt(matcher.group(3));
            int octet4 = Integer.parseInt(matcher.group(4));

            if (octet1 > 255 || octet2 > 255 || octet3 > 255 || octet4 > 255) {
                throw new IllegalArgumentException("Endereço IP inválido.");
            }

            // 127.0.0.0/8 (Loopback)
            if (octet1 == 127) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
            // 0.0.0.0/8
            if (octet1 == 0) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
            // 10.0.0.0/8 (RFC 1918)
            if (octet1 == 10) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
            // 172.16.0.0/12 (RFC 1918)
            if (octet1 == 172 && (octet2 >= 16 && octet2 <= 31)) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
            // 192.168.0.0/16 (RFC 1918)
            if (octet1 == 192 && octet2 == 168) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
            // 169.254.0.0/16 (Link Local)
            if (octet1 == 169 && octet2 == 254) {
                throw new IllegalArgumentException("Endereços locais ou de redes privadas não são permitidos.");
            }
        }
    }

    public static TargetUrl of(String value) {
        return new TargetUrl(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

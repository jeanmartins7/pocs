package com.encurtador.infrastructure.adapter.out.redis;

import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;
import com.encurtador.domain.model.TargetUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisLinkRepositoryAdapterTest {

    @Autowired
    private RedisLinkRepositoryAdapter repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        var keys = redisTemplate.keys("link:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Deve salvar e recuperar um link com sucesso no Redis")
    void shouldSaveAndFindLink() {
        ShortCode code = ShortCode.fromInput("TEST123");
        TargetUrl url = TargetUrl.of("https://example.com/test");
        Instant expires = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        String rawKey = Link.generateManagementKey();

        Link link = Link.create(code, url, expires, rawKey);
        boolean saved = repository.saveIfAbsent(link);

        assertThat(saved).isTrue();

        Optional<Link> found = repository.findByCode(code);
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(code);
        assertThat(found.get().getTargetUrl()).isEqualTo(url);
        assertThat(found.get().getExpiresAtUtc()).isNotNull();
        assertThat(found.get().matchesManagementKey(rawKey)).isTrue();
    }

    @Test
    @DisplayName("Deve impedir inserção de código duplicado com atomicidade")
    void shouldPreventDuplicateCodesAtomically() {
        ShortCode code = ShortCode.fromInput("DUP1234");
        TargetUrl url = TargetUrl.of("https://example.com");

        Link link1 = Link.create(code, url, null, Link.generateManagementKey());
        Link link2 = Link.create(code, url, null, Link.generateManagementKey());

        assertThat(repository.saveIfAbsent(link1)).isTrue();
        assertThat(repository.saveIfAbsent(link2)).isFalse();
    }

    @Test
    @DisplayName("Deve incrementar contador de acessos atomicamente no Redis")
    void shouldIncrementAccessCountAtomically() {
        ShortCode code = ShortCode.fromInput("COUNT11");
        TargetUrl url = TargetUrl.of("https://example.com");
        Link link = Link.create(code, url, null, Link.generateManagementKey());

        repository.saveIfAbsent(link);
        long count1 = repository.incrementAccessCount(code);
        long count2 = repository.incrementAccessCount(code);

        assertThat(count1).isEqualTo(1L);
        assertThat(count2).isEqualTo(2L);

        Optional<Link> found = repository.findByCode(code);
        assertThat(found).isPresent();
        assertThat(found.get().getAccessCount()).isEqualTo(2L);
    }
}

package com.encurtador.infrastructure.adapter.out.redis;

import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;
import com.encurtador.domain.model.TargetUrl;
import com.encurtador.domain.port.out.LinkRepositoryPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class RedisLinkRepositoryAdapter implements LinkRepositoryPort {

    private final StringRedisTemplate redisTemplate;

    public RedisLinkRepositoryAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getLockKey(String code) {
        return "link:unique:" + code;
    }

    private String getHashKey(String code) {
        return "link:" + code;
    }

    @Override
    public boolean saveIfAbsent(Link link) {
        String code = link.getCode().value();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(getLockKey(code), "1");
        if (Boolean.FALSE.equals(acquired)) {
            return false;
        }

        Map<String, String> hash = new HashMap<>();
        hash.put("code", code);
        hash.put("targetUrl", link.getTargetUrl().value());
        hash.put("createdAtUtc", link.getCreatedAtUtc().toString());
        if (link.getExpiresAtUtc() != null) {
            hash.put("expiresAtUtc", link.getExpiresAtUtc().toString());
        }
        hash.put("accessCount", String.valueOf(link.getAccessCount()));
        hash.put("managementKeyHash", link.getManagementKeyHash());

        redisTemplate.opsForHash().putAll(getHashKey(code), hash);
        return true;
    }

    @Override
    public Optional<Link> findByCode(ShortCode code) {
        String codeValue = code.value();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(getHashKey(codeValue));
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        String targetUrlStr = (String) entries.get("targetUrl");
        String createdAtUtcStr = (String) entries.get("createdAtUtc");
        String expiresAtUtcStr = (String) entries.get("expiresAtUtc");
        String accessCountStr = (String) entries.get("accessCount");
        String managementKeyHash = (String) entries.get("managementKeyHash");

        Instant createdAtUtc = Instant.parse(createdAtUtcStr);
        Instant expiresAtUtc = expiresAtUtcStr != null ? Instant.parse(expiresAtUtcStr) : null;
        long accessCount = accessCountStr != null ? Long.parseLong(accessCountStr) : 0L;

        Link link = new Link(
                code,
                TargetUrl.of(targetUrlStr),
                createdAtUtc,
                expiresAtUtc,
                accessCount,
                managementKeyHash
        );
        return Optional.of(link);
    }

    @Override
    public long incrementAccessCount(ShortCode code) {
        return redisTemplate.opsForHash().increment(getHashKey(code.value()), "accessCount", 1);
    }

    @Override
    public boolean existsByCode(ShortCode code) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getLockKey(code.value())));
    }
}

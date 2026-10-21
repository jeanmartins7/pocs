package com.encurtador.infrastructure.adapter.out.cache;

import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;
import com.encurtador.domain.port.out.LinkCachePort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CaffeineLinkCacheAdapter implements LinkCachePort {

    private final Cache<String, Link> cache;

    public CaffeineLinkCacheAdapter() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    @Override
    public Optional<Link> get(ShortCode code) {
        return Optional.ofNullable(cache.getIfPresent(code.value()));
    }

    @Override
    public void put(Link link) {
        cache.put(link.getCode().value(), link);
    }

    @Override
    public void evict(ShortCode code) {
        cache.invalidate(code.value());
    }
}

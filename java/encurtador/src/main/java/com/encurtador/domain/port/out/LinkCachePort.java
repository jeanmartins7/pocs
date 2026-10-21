package com.encurtador.domain.port.out;

import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;

import java.util.Optional;

public interface LinkCachePort {

    Optional<Link> get(ShortCode code);

    void put(Link link);

    void evict(ShortCode code);
}

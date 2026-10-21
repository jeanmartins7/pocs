package com.encurtador.domain.port.out;

import com.encurtador.domain.model.Link;
import com.encurtador.domain.model.ShortCode;

import java.util.Optional;

public interface LinkRepositoryPort {

    Optional<Link> findByCode(ShortCode code);

    boolean saveIfAbsent(Link link);

    long incrementAccessCount(ShortCode code);

    boolean existsByCode(ShortCode code);
}

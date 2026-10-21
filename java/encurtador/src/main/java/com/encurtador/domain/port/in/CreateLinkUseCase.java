package com.encurtador.domain.port.in;

public interface CreateLinkUseCase {
    CreatedLink create(CreateLinkCommand command);
}

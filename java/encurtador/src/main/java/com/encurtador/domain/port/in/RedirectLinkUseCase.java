package com.encurtador.domain.port.in;

public interface RedirectLinkUseCase {
    String getTargetUrlAndIncrementCount(String code);
}

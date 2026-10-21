package com.encurtador.domain.port.in;

public interface GetLinkStatsUseCase {
    LinkStats getStats(String code, String managementKey);
}

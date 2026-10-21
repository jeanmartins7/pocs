package com.encurtador.infrastructure.adapter.in.web;

import com.encurtador.infrastructure.adapter.in.web.dto.CreateLinkRequest;
import com.encurtador.infrastructure.adapter.in.web.dto.CreatedLinkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanRedis() {
        var keys = redisTemplate.keys("link:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Caminho feliz: Criar link automático, redirecionar e consultar estatísticas")
    void fullHappyPathFlow() throws Exception {
        // 1. Criar link com código automático
        CreateLinkRequest createRequest = new CreateLinkRequest(
                "https://example.com/artigo-interessante",
                null,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.managementKey").isNotEmpty())
                .andReturn();

        CreatedLinkResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CreatedLinkResponse.class
        );

        String code = created.code();
        String managementKey = created.managementKey();
        assertThat(code).hasSize(7);

        // 2. Redirecionar por código (HTTP 302 com no-cache)
        mockMvc.perform(get("/" + code.toLowerCase())) // teste de insensibilidade à caixa (RN-05)
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/artigo-interessante"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"));

        // Redirecionar uma segunda vez
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/artigo-interessante"));

        // 3. Consultar estatísticas com a chave de gerenciamento
        mockMvc.perform(get("/api/links/" + code + "/stats")
                        .header("X-Management-Key", managementKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com/artigo-interessante"))
                .andExpect(jsonPath("$.accessCount").value(2));
    }

    @Test
    @DisplayName("Caminho feliz: Criar link com código próprio (T-06)")
    void shouldCreateLinkWithCustomCode() throws Exception {
        CreateLinkRequest createRequest = new CreateLinkRequest(
                "https://example.com/custom",
                "CUSTOMCODE99",
                null
        );

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/CUSTOMCODE99"))
                .andExpect(jsonPath("$.code").value("CUSTOMCODE99"));
    }

    @Test
    @DisplayName("Conflito: Rejeitar código próprio já em uso com 409 ProblemDetails (T-06)")
    void shouldReturn409ForDuplicateCustomCode() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "UNIQUECODE", null);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Código já em uso"));
    }

    @Test
    @DisplayName("Validação: Rejeitar URL privada com 400 ProblemDetails (T-04, RN-02)")
    void shouldReturn400ForPrivateUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("http://192.168.1.1/router", null, null);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Erro de validação"));
    }

    @Test
    @DisplayName("Redirecionamento: Retornar 404 para código inexistente (T-07)")
    void shouldReturn404ForNonExistentCode() throws Exception {
        mockMvc.perform(get("/NOTFOUND1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Redirecionamento: Retornar 410 para link expirado (T-07, RN-03)")
    void shouldReturn410ForExpiredLink() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        // Persistir link diretamente expirado
        com.encurtador.domain.model.Link expired = new com.encurtador.domain.model.Link(
                com.encurtador.domain.model.ShortCode.fromInput("EXPIRED1"),
                com.encurtador.domain.model.TargetUrl.of("https://example.com"),
                past.minus(1, ChronoUnit.DAYS),
                past,
                0,
                "hash"
        );
        new com.encurtador.infrastructure.adapter.out.redis.RedisLinkRepositoryAdapter(redisTemplate)
                .saveIfAbsent(expired);

        mockMvc.perform(get("/EXPIRED1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.title").value("Link expirado"));
    }

    @Test
    @DisplayName("Estatísticas: Retornar 401 quando chave estiver incorreta ou ausente (T-09)")
    void shouldReturn401ForWrongOrMissingManagementKey() throws Exception {
        CreateLinkRequest createRequest = new CreateLinkRequest("https://example.com", "STATSTEST", null);
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Sem header
        mockMvc.perform(get("/api/links/STATSTEST/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Com chave incorreta
        mockMvc.perform(get("/api/links/STATSTEST/stats")
                        .header("X-Management-Key", "CHAVE_ERRADA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Concorrência: Acessos concorrentes devem incrementar contador atomicamente sem perder contagem (T-08, D-02)")
    void concurrentRedirectsShouldIncrementAtomically() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com/concurrency", "CONCURR1", null);
        MvcResult res = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CreatedLinkResponse created = objectMapper.readValue(res.getResponse().getContentAsString(), CreatedLinkResponse.class);

        int totalThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(get("/CONCURR1"))
                            .andExpect(status().isFound());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verificar contagem atômica
        mockMvc.perform(get("/api/links/CONCURR1/stats")
                        .header("X-Management-Key", created.managementKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessCount").value(totalThreads));
    }
}

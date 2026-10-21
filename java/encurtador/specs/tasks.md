# Tasks: Encurtador de links

## Phase 1: Setup
- [X] T-01 Criar projeto e estrutura de pastas (Hexagonal Architecture, Spring Boot 3, Java 24/25, Maven, Redis, JUnit 5, Pitest)

## Phase 2: Foundational & Core Domain
- [X] T-02 Modelar a entidade Link e persistência no Redis (RF-01, RF-08)
- [X] T-03 [P] Gerar código aleatório de 7 caracteres (RF-02, D-03)
- [X] T-04 [P] Validar URL de destino (RN-02, borda 2048)

## Phase 3: Link Creation & Custom Code
- [X] T-05 Criar link com código automático (RF-01, T-02, T-03, T-04)
- [X] T-06 Aceitar código próprio com unicidade (RF-03, RN-01, D-04)

## Phase 4: Redirection & Atomic Counting
- [X] T-07 Redirecionar por código (RF-04, RN-03, RN-05, 302 / 404 / 410)
- [X] T-08 Incrementar contador de forma atômica (RF-05, RN-04, D-02)

## Phase 5: Stats & Optimization
- [X] T-09 Endpoint de estatísticas com chave (RF-07, DC-03, D-05)
- [X] T-10 Cache em memória dos links mais acessados (T-07)

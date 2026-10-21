# Tarefas

| ID | Tarefa | Origem | Depende de | Concluída quando | Status |
|---|---|---|---|---|---|
| T-01 | Criar projeto e estrutura de pastas | Constituição | | Projeto compila | [X] Concluída |
| T-02 | Modelar a entidade Link e persistência no Redis | RF-01, RF-08 | T-01 | Persistência salva e recupera no Redis | [X] Concluída |
| T-03 | Gerar código aleatório de 7 caracteres | RF-02, D-03 | T-01 | Teste unitário cobre formato e alfabeto | [X] Concluída |
| T-04 | Validar URL de destino | RN-02, borda 2048 | T-01 | Teste cobre http, https e inválidos | [X] Concluída |
| T-05 | Criar link com código automático | RF-01 | T-02, T-03, T-04 | Teste de integração cria e retorna 201 | [X] Concluída |
| T-06 | Aceitar código próprio com unicidade | RF-03, RN-01, D-04 | T-05 | Código duplicado retorna 409 | [X] Concluída |
| T-07 | Redirecionar por código | RF-04 | T-05 | Teste de integração retorna 302 | [X] Concluída |
| T-08 | Incrementar contador de forma atômica | RF-05, RN-04, D-02 | T-07 | Teste concorrente não perde contagem | [X] Concluída |
| T-09 | Endpoint de estatísticas com chave | RF-07, DC-03 | T-05 | Chave errada retorna 401 | [X] Concluída |
| T-10 | Cache em memória dos links mais acessados | | T-07 | Latência de redirect cai | [X] Concluída |
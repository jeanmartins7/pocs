# Constituição do projeto

## Stack
- java 25, Spring boot, Minimal API
- Persistência em redis
- Testes com junit5
- Testes de mutação com pitest

## Arquitetura
- Endpoint não acessa banco diretamente, sempre via serviço de aplicação
- Utilize arquitetura hexagonal
- Regra de negócio não vive no endpoint nem no handler HTTP
- Nenhuma dependência nova entra sem justificativa escrita no plano

## Qualidade
- Toda regra de negócio precisa ter teste unitário
- Todo teste unitário deve ter pelo menos um teste de mutação
- Todo endpoint precisa ter ao menos um teste de integração de caminho feliz
- Erro de validação sempre retorna 400 com ProblemDetails

## Convenções
- Nomes de classe e arquivo em inglês, mensagens de erro em português
- Commit no formato: tipo(escopo): descrição, com o identificador do requisito

## Governança
- Se o plano contradiz a constituição, quem muda é o plano
- Se a constituição precisa mudar, muda em commit separado
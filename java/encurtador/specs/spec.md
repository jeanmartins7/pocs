# Encurtador de links

## Problema
Links longos quebram em mensagens, e-mails e materiais impressos, e não dá para
medir quantas pessoas clicaram neles.

## Objetivo
Permitir que uma pessoa transforme uma URL longa em um link curto, compartilhe
esse link e acompanhe quantos acessos ele recebeu.

## Clarifications

### Session 2026-10-20
- Q: Como o sistema deve validar se a URL de destino é "publicamente acessível" ao criar o link? (RN-02) → A: Validar a sintaxe da URL e bloquear domínios/IPs locais e privados (como localhost, 127.0.0.1 e faixas RFC 1918) sem realizar requisição de rede externa.
- Q: Como o sistema deve tratar o código do link ao receber uma requisição de acesso com letras minúsculas na URL? (RN-05) → A: Converter automaticamente o código recebido para maiúsculas antes de buscar no banco (acesso insensível à caixa).
- Q: Qual resposta o sistema deve retornar ao visitante quando ele tentar acessar um link com data de validade expirada? (RN-03) → A: Retornar status HTTP 410 (Gone) com mensagem indicando que o link está expirado.
- Q: Quais informações devem ser retornadas na consulta de estatísticas do link além do total de acessos? (RF-07) → A: Contagem de acessos acompanhada dos metadados do link (código, URL de destino, data de criação e data de expiração, se houver).
- Q: Qual status HTTP o sistema deve retornar ao redirecionar o visitante para a URL de destino? (RF-04) → A: HTTP 302 (Found) sem cache no cliente, garantindo a contagem individual de cada acesso.

## Usuários
- Criador do link: gera links curtos e acompanha os acessos
- Visitante: clica no link curto e chega ao destino

## Histórias
- Como criador, quero encurtar uma URL para compartilhar um link menor
- Como criador, quero escolher o código do link para usar um nome legível
- Como criador, quero ver quantos acessos meu link recebeu
- Como visitante, quero ser redirecionado ao destino sem passar por tela intermediária

## Requisitos funcionais
- RF-01: o sistema recebe uma URL de destino e devolve um link curto
- RF-02: o código do link tem 7 caracteres gerados automaticamente, usando apenas
  letras maiúsculas de A a Z e números de 1 a 9
- RF-03: o criador pode informar um código próprio de 4 a 20 caracteres, usando
  apenas letras maiúsculas de A a Z e números de 1 a 9, sem espaços
- RF-04: acessar um link curto existente redireciona para a URL de destino com
  status HTTP 302 (Found) e cabeçalhos para desabilitar cache no cliente
- RF-05: cada acesso a um link válido incrementa o contador daquele link,
  incluindo acessos repetidos e automatizados
- RF-06: o criador pode definir uma data de expiração para o link; a data e o
  limite de validade são interpretados em UTC, e o link permanece válido até o
  fim da data informada
- RF-07: o sistema devolve o total de acessos e os metadados do link (código,
  URL de destino, data de criação em UTC e data de expiração, se houver) a
  qualquer pessoa que conheça seu código
- RF-08: os links e seus dados devem ser persistidos em banco de dados para que
  não se percam após reinício da aplicação

## Regras de negócio
- RN-01: o código do link é único em todo o sistema
- RN-02: a URL de destino precisa usar http ou https e ter formato sintático válido, não apontando para endereços locais ou redes privadas (ex.: localhost, 127.0.0.1, faixas RFC 1918), validado sem requisição de rede externa
- RN-03: link expirado não redireciona e não conta acesso, respondendo com HTTP 410 (Gone)
- RN-04: o contador de acessos nunca diminui
- RN-05: códigos automáticos e personalizados aceitam somente A-Z maiúsculo e
  os dígitos de 1 a 9 na criação; o dígito 0, espaços e caracteres especiais
  não são permitidos. No momento do acesso ou consulta de estatísticas, letras minúsculas
  são convertidas automaticamente para maiúsculas (insensível à caixa na leitura)
- RN-06: se um código gerado automaticamente já estiver em uso, o sistema tenta
  gerar outro código

## Casos de borda
- Código já em uso: recusar a criação e explicar o motivo
- Código gerado automaticamente já em uso: gerar outro código e tentar
  novamente
- Código inexistente no acesso: responder com HTTP 404 informando que o link não existe
- Código de link expirado no acesso: responder com HTTP 410 informando que o link expirou
- URL de destino com mais de 2048 caracteres: recusar
- URL de destino apontando para endereço local ou rede privada: recusar
- Data de expiração no passado: recusar na criação

## Fora de escopo
- Cadastro e login de usuários
- Domínio personalizado
- Relatório com origem geográfica do acesso
- Edição do destino de um link já criado

## Critérios de aceite
- Criar um link e acessá-lo redireciona para o destino informado com status HTTP 302 (Found)
- Dois links não podem ter o mesmo código
- Link com data de expiração vencida responde erro HTTP 410 (Gone) e não incrementa o contador
- Um link ainda válido funciona até o fim de sua data de expiração em UTC
- Qualquer pessoa que conheça o código pode consultar o total de acessos e metadados informativos do link
- Após reiniciar a aplicação, os links persistidos no banco continuam funcionando
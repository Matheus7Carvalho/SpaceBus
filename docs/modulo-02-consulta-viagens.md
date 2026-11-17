# Módulo 2 — Consulta de viagens (passageiro)

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.1 e Fluxo A (passos 1 a 3). Atende ao requisito de aceite 1 do MVP: *o passageiro encontra uma viagem ativa por origem, destino e data, ou recebe uma mensagem clara de ausência de opções*.

Perfis: todos os autenticados (a busca é a tela inicial de Passageiro e Motorista). Operador e Administrador continuam começando em Viagens.

## Premissas adotadas (decisões pendentes do escopo)

- **Busca exige login.** O frontend inteiro fica atrás do login; abrir a busca sem conta é reversível depois (endpoint de leitura próprio, sem dado pessoal).
- **Transportadora**: o piloto tem uma. O nome exibido vem da configuração `SPACEBUS_TRANSPORTADORA` (padrão "Transportadora piloto") até existir o cadastro de transportadoras.
- **Disponibilidade** = capacidade da viagem − reservas confirmadas. Sem o módulo de reservas, hoje é igual à capacidade; o cálculo fica num único ponto do serviço para o módulo 3 completar.

## Histórias e critérios de aceite (PO)

### H5 — Buscar viagens por origem, destino e data

Como passageiro, quero informar de onde saio, para onde vou e o dia, para ver as partidas disponíveis.

- [x] Origem, destino e data obrigatórios; origem e destino com até 100 caracteres; data de hoje em diante ("Escolha hoje ou uma data futura.").
- [x] Origem e destino são procurados no nome dos pontos de parada, ignorando maiúsculas e acentos; a origem da rota conta como o primeiro ponto e o destino da rota como o último.
- [x] Só aparece a viagem em que o ponto de embarque vem **antes** do ponto de desembarque no percurso (não se viaja "ao contrário").
- [x] Só viagens **publicadas** com partida **depois de agora** (rascunho, suspensa, cancelada e partidas passadas não aparecem).
- [x] Cada resultado mostra: transportadora, horário, preço, embarque e desembarque (com referência), tipo de veículo e lugares disponíveis; ordenado por horário.
- [x] Sem resultado: "Nenhuma viagem encontrada" com orientação para mudar a data ou os locais.
- [x] Os campos sugerem os locais atendidos (pontos de rotas com viagens publicadas futuras).
- [x] A busca fica na URL (recarregar e voltar do detalhe mantêm o resultado).

### H6 — Ver detalhes da viagem

Como passageiro, quero ver a rota completa e os pontos de parada, para conferir onde embarcar.

- [x] Mostra rota, horário, preço, veículo, lugares disponíveis e todos os pontos na ordem, destacando embarque e desembarque escolhidos na busca.
- [x] Informação ausente aparece como tal ("Referência não informada"), nunca vazia ou inventada.
- [x] Mostra quando a viagem foi atualizada pela transportadora.
- [x] Viagem que não está mais publicada (ou já partiu) mostra "Esta viagem não está mais disponível." em vez dos dados.

### H7 — Lista do operador filtrada no servidor (pendência D2 do módulo 1)

- [x] A lista de viagens do operador é filtrada e paginada no servidor (dia, rota, situação).
- [x] Sem filtro de dia, mostra as viagens de hoje em diante; com dia, as daquele dia (inclusive passadas).
- [x] "Mostrar mais" carrega a próxima página; a tela informa quantas existem.

## Fora deste módulo

- Reserva, escolha de lugares e pagamento (módulo 3). A tela de detalhe não mostra botão de reserva até lá.
- Mapa e localização da van (módulo 4).
- Busca sem login e mais de uma transportadora.

## Plano de teste (QA)

| Camada | Onde | O que cobre |
|---|---|---|
| Serviço | `BuscaViagemServiceTest` | Casamento de origem/destino (acentos, ordem do percurso, origem/destino da rota), só publicadas e futuras, data passada, detalhe indisponível, locais |
| Controller | `BuscaViagemControllerTest`, `ViagemControllerTest` | Todos os perfis buscam; 401 sem sessão; validação de parâmetros; agenda do operador com filtros e 403 |
| E2E | `e2e/busca.spec.ts`, `e2e/viagens.spec.ts` | Passageiro busca, vê resultado e detalhe, sem resultado, URL; viagem suspensa some da busca; lista do operador com filtros no servidor |

Roteiro exploratório:

1. Entrar como **Passageiro**: cai em "Buscar viagens".
2. Buscar "centro" → "aeroporto" para amanhã: aparecem as duas viagens publicadas da seed, com embarque "Terminal Central" e desembarque "Aeroporto".
3. Buscar "aeroporto" → "centro": nenhuma viagem (sentido contrário).
4. Buscar "praca" → "shopping" (sem acento): embarque "Praça Principal".
5. Abrir uma viagem: conferir pontos, destaque e "Referência não informada" onde faltar.
6. Como **Operador**, suspender uma das viagens e repetir a busca: ela some; o link de detalhe aberto antes informa que não está mais disponível.

## Validação (17/11/2026)

- Backend `./mvnw test`: 144 testes, todos passando.
- Frontend `tsc -b` e lint: ok.
- E2E Playwright (desktop + mobile), suíte completa: 145 passaram, 0 falharam (1 pulado: gaveta do menu, só existe no celular).

**Veredito do QA: Aprovado com ressalvas.**

| # | Ressalva | Severidade | Situação |
|---|---|---|---|
| D1 | Casamento escolhia o primeiro ponto que *contém* o texto (ex.: "Rua do Aeroporto Velho" em vez de "Aeroporto"); a origem da rota colada ao 1º ponto vencia um ponto com o mesmo nome | Média | Corrigido: nota por relevância (igual > começa com > contém > origem/destino da rota). Testes de serviço |
| D2 | "Mostrar mais" aumentava o tamanho da página (teto do Spring em 2000) e baixava tudo de novo | Baixa | Corrigido: paginação real (`useInfiniteQuery`), uma página por clique. E2E com duas páginas simuladas |
| D3 | `sort` livre na agenda (campo inválido virava erro técnico) | Baixa | Corrigido: ordem fixa por partida e 100 por página no servidor |
| D4 | Data malformada mostrava mensagem técnica do Spring | Baixa | Corrigido para toda a API: "Informe um valor válido." |
| D6 | Editar rota não atualizava a busca nem a agenda em cache | Baixa | Corrigido |
| D7 | Nome acessível do resultado diferente do texto visível (WCAG 2.5.3) | Baixa | Corrigido |
| — | Lacunas: filtros de rota/situação no banco, "Mostrar mais", ordem dos resultados, asserção da viagem suspensa | — | Cobertas por E2E |
| D5 | "Hoje" do campo de data usa o fuso do navegador; o servidor usa `APP_FUSO_HORARIO` | Baixa | **Pendente** (só afeta quem está em outro fuso perto da meia-noite; o servidor continua a regra) |
| D8 | E2E deixa rotas `E2E-…` e viagens canceladas no banco de dev (também no módulo 1) | Média (higiene) | **Pendente**: endpoint de limpeza por marca, só no perfil dev |
| D9 | `locais()` e a busca carregam viagens com rota/pontos por consulta | Baixa | **Pendente**: aceitável no volume do piloto; rever com dados reais |

Lacuna registrada: as consultas JPQL do caso "partida ainda hoje" não têm teste com banco (`@DataJpaTest` com Postgres em container entra quando o projeto adotar Testcontainers).

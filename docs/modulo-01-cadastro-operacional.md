# Módulo 1 — Cadastro operacional da transportadora

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.4 e Fluxo B (passos 1 e 2). Atende ao requisito de aceite 6 do MVP: *o operador cadastra e altera horários, capacidade e estado de uma viagem sem intervenção técnica*.

Perfis: **Operador** e **Administrador**. Motorista e Passageiro não acessam (bloqueio no servidor e na tela).

## Histórias e critérios de aceite (PO)

### H1 — Cadastrar veículo

Como operador, quero cadastrar os veículos da frota, para escolher qual faz cada viagem.

- [x] Campos: placa, descrição (modelo), tipo (Van/Ônibus), capacidade (1–60 lugares), ativo.
- [x] Placa aceita com ou sem hífen e minúsculas; é gravada sem hífen, em maiúsculas. Aceita padrão antigo (ABC1234) e Mercosul (ABC1D23); outro formato é recusado com "Informe uma placa válida (ex.: ABC1D23)."
- [x] Placa única: "Já existe um veículo com a placa …".
- [x] Não é possível reduzir a capacidade abaixo da de uma viagem futura não cancelada do veículo.
- [x] Veículo com viagens não pode ser excluído (orientação: inativar).
- [x] Veículo inativo não pode ser usado em viagem nova nem ter viagem publicada.
- [x] Listagem com filtros por placa/descrição, tipo e situação, guardados na URL.

### H2 — Cadastrar rota com pontos de parada

Como operador, quero cadastrar a rota com os pontos de embarque na ordem do percurso, para o passageiro saber onde embarcar.

- [x] Campos: nome (único), origem, destino e de 2 a 30 pontos de parada (nome obrigatório, referência opcional).
- [x] A ordem da tela é a ordem gravada; pontos podem ser movidos (↑ ↓) e removidos (mínimo 2).
- [x] Enter no último ponto cria um novo ponto.
- [x] Rota com viagens não pode ser excluída.
- [x] Listagem filtra por nome e por local (origem, destino ou qualquer ponto).

### H3 — Programar viagem

Como operador, quero criar uma viagem (rota, veículo, partida, preço, capacidade e, opcionalmente, motorista), para depois publicá-la.

- [x] Viagem nova nasce como **Rascunho**.
- [x] Ao escolher o veículo, a capacidade é preenchida com a do veículo (se vazia ou maior).
- [x] Capacidade não passa da do veículo; preço entre R$ 0,01 e R$ 9.999,99, com até 2 casas.
- [x] Partida precisa ser no futuro; segundos são descartados.
- [x] O mesmo veículo não pode ter duas viagens não canceladas no mesmo horário de partida.
- [x] Motorista é opcional ("A definir"); se informado, precisa ser usuário Motorista ativo.
- [x] A viagem pode ser editada (horário, capacidade, preço…) em qualquer situação, exceto Cancelada.

### H4 — Publicar, suspender e cancelar viagem

Como operador, quero controlar a situação da viagem, para que só o que está confirmado fique disponível ao passageiro.

- [x] Situações: `RASCUNHO → PUBLICADA ⇄ SUSPENSA`; qualquer uma → `CANCELADA` (definitivo).
- [x] Publicar revalida as regras (partida futura, veículo ativo, capacidade, conflito de horário).
- [x] Cada ação pede confirmação; Cancelar avisa que não pode ser desfeito.
- [x] As ações ficam bloqueadas enquanto houver alterações não salvas (valeriam para a versão gravada).
- [x] Viagem cancelada abre somente para leitura.
- [x] Só rascunho pode ser excluído; o que já foi oferecido sai do ar por cancelamento.
- [x] Listagem por partida, com filtros por dia, rota e situação.

## Fora deste módulo (próximos)

- Busca do passageiro e vagas disponíveis (dependem do módulo de reserva). Hoje a capacidade é a quantidade à venda; "vagas disponíveis" virá de capacidade − reservas confirmadas.
- Avisar passageiros quando a viagem for alterada ou cancelada (módulos de reserva e notificação).
- Histórico/auditoria das mudanças de situação (exigido para acesso administrativo; entra com reserva, quando houver passageiros afetados).
- Mais de uma transportadora: o piloto tem uma; quando houver outra, veículos, rotas e viagens passam a pertencer a uma transportadora e o operador só vê as suas.

## Plano de teste (QA)

Automatizado:

| Camada | Onde | O que cobre |
|---|---|---|
| Serviço (JUnit + Mockito) | `backend/src/test/.../service/{veiculo,rota,viagem}` | Todas as regras acima, inclusive cada mensagem de erro e o que não é salvo |
| Controller (`@WebMvcTest`) | `backend/src/test/.../controller/{veiculo,rota,viagem}` | 401 sem sessão, 403 para Motorista/Passageiro (inclusive nas transições), validação e limites, formato do JSON |
| Guarda de limites | `LimitesDosRequestsTest`, `e2e/limites.spec.ts` | Nenhum campo sem limite |
| E2E (Playwright, desktop e mobile) | `frontend/e2e/{veiculos,rotas,viagens,auth}.spec.ts` | Fluxos de cadastro, erros do servidor na tela, descarte, filtros na URL, ciclo de situação, permissões por perfil |

Roteiro exploratório sugerido (manual):

1. Entrar como **Operador** → Veículos → cadastrar uma van com placa `abc-1d23`.
2. Rotas → Nova rota com 3 pontos; reordenar; salvar; reabrir e conferir a ordem.
3. Viagens → Nova viagem com a rota e a van; conferir a capacidade preenchida; salvar (Rascunho).
4. Publicar → Suspender → Publicar → Cancelar; conferir que a cancelada fica só leitura.
5. Tentar: partida no passado; capacidade maior que a da van; mesma van no mesmo horário; excluir a van usada.
6. Inativar a van e tentar publicar uma viagem suspensa dela.
7. Entrar como **Passageiro** e **Motorista**: o menu não mostra a operação e as URLs `/operacao/viagens` e `/cadastros/veiculos` são negadas.

## Validação (17/11/2026)

Resultados das execuções:

- Backend `./mvnw test`: 104 testes, todos passando.
- Frontend `tsc -b`, lint e build: ok.
- E2E Playwright (desktop + mobile): 119 passaram e 2 falharam na rodada completa. As 2 falhas eram instabilidades do próprio teste, registradas em [testes-instaveis.md](testes-instaveis.md). Depois da correção, os specs de veículos, rotas e viagens passaram 80/80 em duas repetições.

**Veredito do QA: Aprovado com ressalvas.**

Defeitos apontados pelo QA:

| # | Defeito | Severidade | Situação |
|---|---|---|---|
| D1 | Placa ou nome de rota repetido **na edição** dava erro genérico. O flush gravava antes da checagem de unicidade. | Média | Corrigido: a validação roda antes de alterar a entidade. Coberto por teste de serviço e E2E. |
| D3 | Inativar um veículo não afetava as viagens já publicadas. | Média | Corrigido: veículo com viagens futuras não canceladas não pode ser inativado. Coberto por teste de serviço e E2E. |
| D4 | O conflito "mesmo veículo, mesmo horário" era checado só na aplicação, o que falha sob concorrência. | Baixa/média | Corrigido: índice único parcial `uk_viagem_veiculo_partida_ativa`. |
| D6 | O `nginx.conf` ainda tinha um resto do SmartMall (limite de upload). | Baixa | Corrigido. |
| D2 | A lista de viagens busca no máximo 500 registros e filtra no cliente. | Média | **Pendente.** Paginação e filtro no servidor entram com a busca do passageiro (módulo 2). |
| D5 | As partidas da seed são calculadas na carga e ficam no passado depois de 1 a 3 dias. | Baixa | **Pendente.** Para renovar: `docker compose down -v` e subir o backend de novo. |

Lacunas de teste registradas para a próxima iteração:

- E2E dos filtros por dia e por rota.
- Testes de integração (com banco) das consultas `maiorCapacidadeFutura` e `existeConflitoVeiculo`.
- E2E de remover ponto de parada.

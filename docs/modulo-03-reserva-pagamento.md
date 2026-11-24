# Módulo 3 — Reserva e pagamento

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.3 e Fluxo A (passos 4 a 6). Requisitos de aceite do MVP atendidos: 2 (disponibilidade consistente, sem dupla venda), 3 (aprovado só após o provedor, sem cobrança duplicada), 4 (consultar a reserva depois de sair e voltar) e parte do 7 (cancelamento identifica os passageiros a avisar).

## Premissas adotadas (decisões pendentes do escopo, seção 9)

| Decisão pendente | Premissa | Onde mudar |
|---|---|---|
| Assento numerado ou quantidade | **Quantidade de lugares** (1 a 6 por reserva) | `ReservaRequest`, `ReservaService.MAX_LUGARES` |
| Prazo de bloqueio da vaga | **10 minutos** para pagar | `spacebus.reserva.bloqueio-minutos` |
| Provedor e método de pagamento | **Provedor simulado** (Pix de teste) atrás da interface `ProvedorPagamento`; nenhum dado de cartão é recebido nem guardado | trocar a implementação de `ProvedorPagamento` |
| Transações reais ou simuladas | **Simuladas**: a tela mostra "Simulado" no pagamento | idem |
| Cancelamento e reembolso | Passageiro cancela até **2 horas antes** da partida; pagamento aprovado é reembolsado integralmente (no provedor simulado) | `spacebus.reserva.cancelamento-horas` |

Essas premissas precisam ser confirmadas com a transportadora antes de aceitar reservas reais.

## Histórias e critérios de aceite (PO)

### H9 — Reservar lugares

Como passageiro, quero reservar lugares numa viagem, para garantir minha vaga.

- [x] No detalhe da viagem, escolho a quantidade (1 a 6, até os lugares disponíveis), vejo o total e a política de cancelamento antes de confirmar.
- [x] A reserva recebe um **código** (ex.: `SB-7K3Q9M`), fica ligada à viagem, ao passageiro e ao trecho (embarque → desembarque) e nasce **aguardando pagamento**, com prazo de 10 minutos.
- [x] Durante o prazo, os lugares ficam bloqueados: somem da disponibilidade para os outros passageiros.
- [x] **Nunca há dupla venda**: duas reservas simultâneas da última vaga resultam em uma reserva e um aviso "Não há lugares suficientes" para a outra (trava no banco por viagem).
- [x] Só se reserva viagem publicada com partida futura; só o perfil Passageiro reserva.
- [x] Lugares disponíveis = capacidade − (confirmadas + aguardando pagamento dentro do prazo), igual na busca, no detalhe e na reserva.

### H10 — Pagar pelo provedor

Como passageiro, quero pagar a reserva, para confirmá-la.

- [x] O pagamento é criado no provedor e o passageiro é levado à tela do provedor (simulada).
- [x] A reserva só fica **confirmada** depois do retorno de aprovação do provedor (webhook), nunca pela tela do passageiro.
- [x] Retorno repetido do provedor não muda nada (idempotente); pagamento recusado mantém a reserva aguardando, dentro do prazo, para nova tentativa; não existem duas cobranças pendentes para a mesma reserva.
- [x] Prazo vencido: a reserva expira e libera os lugares. Aprovação que chega depois do prazo confirma se ainda houver lugares; senão a reserva fica expirada e o pagamento é marcado **para reembolso**.
- [x] Falhas do provedor aparecem como mensagem clara e não mudam o estado da reserva.

### H11 — Minhas reservas

- [x] O passageiro vê a lista das suas reservas (código, viagem, trecho, quantidade, total, situação da reserva e do pagamento) e o detalhe de cada uma, inclusive depois de sair e entrar de novo.
- [x] Pode cancelar até 2 horas antes da partida; se pago, o pagamento vai para reembolso. Um passageiro não vê reservas de outro.

### H12 — Reservas na operação

- [x] Na edição da viagem, o operador vê as reservas e a lista de passageiros (nome, trecho, quantidade, situação) e o total de lugares vendidos.
- [x] Não é possível reduzir a capacidade abaixo dos lugares já reservados.
- [x] Ao cancelar uma viagem com reservas, as reservas são canceladas, os pagamentos aprovados vão para reembolso e a tela lista os passageiros a avisar.
- [x] Suspender uma viagem impede novas reservas; as existentes continuam.

## Fora deste módulo

- Provedor de pagamento real, Pix/cartão de produção e conciliação financeira.
- Notificação ao passageiro (módulo 5); hoje a lista "a avisar" fica na tela do operador.
- Assento numerado.

## Plano de teste (QA)

| Camada | O que cobre |
|---|---|
| Serviço | Disponibilidade, bloqueio e expiração, todas as transições de reserva e pagamento, idempotência do webhook, aprovação fora do prazo, cancelamento com e sem pagamento, permissões de dono |
| Controller | 401/403 por perfil, validação, webhook só com segredo do provedor |
| E2E | Reserva → pagamento simulado aprovado → confirmada; recusa e nova tentativa; minhas reservas após sair/entrar; cancelamento; operador vê passageiros; **duas reservas simultâneas da última vaga** |

## Validação

### Rodada 1 do QA (23/11/2026): Reprovado

| # | Defeito | Severidade | Correção |
|---|---|---|---|
| D1 | O retorno do provedor lia reserva e viagem antes da trava e decidia com dados antigos (venda acima da capacidade; reserva confirmada em viagem cancelada) | Alta | Ordem única de travas **viagem → reserva → pagamento** em todas as operações. Os ids são descobertos por consulta escalar e só depois de travar os dados são lidos. Provado com PostgreSQL real (`ReservaConcorrenciaIntegracaoTest`); com a leitura antiga reintroduzida, o teste falha. |
| D2 | Sem histórico de status de reserva e pagamento | Alta | Tabela `reserva_historico` (ação, origem, responsável, data, detalhe) gravada em toda mudança e exibida ao passageiro. O acesso do operador à lista de passageiros fica no log `auditoria`. |
| D3 | Cancelamento do passageiro sem trava, junto com o retorno do provedor | Média | Cancelamento trava viagem e reserva; teste de integração. |
| D4 | Reembolso que falhava ficava sem marcação | Média | Situação `REEMBOLSO_PENDENTE` e nova tentativa automática a cada 5 min. |
| D5 | Segredo do webhook e provedor simulado com valor padrão em qualquer perfil | Média | Valores padrão só nos perfis `dev` e `docker`; fora deles o backend não sobe sem configurar. |
| D8 | Início de pagamento concorrente criava cobrança órfã | Baixa | Início do pagamento passa pela trava; a segunda chamada devolve a cobrança pendente. |
| D9 | "Até 2 horas antes" excluía o limite | Baixa | Limite inclusivo (exatamente 2 h antes ainda pode). |
| D10 | Changeset de renovação da seed | Baixa | Não move viagem com reservas; contextos do Liquibase padrão `producao` fora de dev/docker (seeds não rodam). |
| D6, D7 | Rota Fixa: edição refazia rascunhos; agendador numa transação só | Baixa | Só refaz se a programação mudou; uma transação por rotina. |

Testes depois das correções: backend 219 (inclui 5 de integração com PostgreSQL real), E2E 161 passaram e 0 falharam.

### Rodada 2 do QA (23/11/2026): **Aprovado com ressalvas**

Os defeitos da rodada 1 foram todos resolvidos e estão cobertos por teste, inclusive de integração com PostgreSQL real.

Ressalvas novas:

| # | Ressalva | Severidade | Situação |
|---|---|---|---|
| N1 | Aprovação que chegava depois da partida confirmava a reserva | Média | Corrigido: só confirma se a viagem ainda vai partir; senão expira e reembolsa |
| N3 | Segredo do webhook em branco liberava cabeçalho vazio | Baixa | Corrigido: o backend não sobe com segredo vazio |
| N4 | Cancelar a viagem podia sobrescrever uma expiração em andamento | Baixa | Corrigido: cada reserva é relida com trava antes de cancelar |
| N5 | Ordem do histórico podia empatar na mesma transação | Baixa | Corrigido: coluna `ordem` (identity do banco) |
| N6 | Cancelar reserva vencida mostrava a mensagem das 2 horas | Baixa | Corrigido: mensagem do prazo de pagamento |
| N2 | Chamada ao provedor (criar cobrança, reembolsar) acontece com a trava da viagem segura e sem timeout | Média | Corrigido em 24/11 (ver abaixo) |
| N7 | Acesso à lista de passageiros registrado só em log (`auditoria`), não persistido | Baixa | Corrigido em 24/11: tabela `auditoria_acesso` (usuário, perfil, ação, viagem, data), gravada antes de a lista sair. A tela de auditoria fica para quando houver demanda |

Lacunas de teste registradas: integração de expiração em lote × retorno do provedor, e de cancelamento da viagem × expiração. **Cobertas em 24/11** (abaixo).

### Fechamento das ressalvas (24/11/2026)

**N2: provedor fora da trava, com tempo limite e tentativas limitadas.**

- **Porta única para o provedor.** Toda chamada passa por `ClienteProvedorPagamento`, que tem tempo limite (`SPACEBUS_PAGAMENTO_TIMEOUT_MS`, padrão 5 s). Tempo esgotado ou erro vira `ProvedorPagamentoException`.
- **Criar cobrança.** São duas transações curtas, com o provedor chamado entre elas:
  1. trava, confere a reserva e calcula a chave de idempotência (reserva + nº de cobranças);
  2. trava de novo, confere outra vez e grava.
- **Chamadas simultâneas** recebem a mesma cobrança (chave igual). Se a reserva vencer enquanto o provedor responde, a cobrança não é gravada.
- **Reembolso.** A transação só marca `REEMBOLSO_PENDENTE` e publica `ReembolsoSolicitado`. O `ProcessadorReembolsos` chama o provedor depois do commit, sem trava, e grava o resultado travando só a cobrança.
- **Falha no reembolso.** Conta uma tentativa. A tarefa agendada tenta de novo até `SPACEBUS_REEMBOLSO_MAX_TENTATIVAS` (padrão 12, uma por 5 min); depois disso, log de erro para ação manual. O histórico mostra "Reembolso pendente" uma vez só.

**Defeito novo encontrado pelos testes de integração (corrigido).** O cancelamento da viagem lia as reservas antes de travá-las. O `travar` devolvia a entidade que já estava na sessão do Hibernate, com a situação antiga. Assim, uma reserva expirada no meio do cancelamento voltava a ser marcada como cancelada: a correção da N4 não tinha efeito real. Agora os ids vêm de uma consulta escalar (`idsDaViagemComStatus`) e cada reserva é lida já travada, como manda a ordem de travas.

**Dados de dev.** O changeset que renova a seed prendia no passado a viagem com qualquer reserva, mesmo cancelada. Agora só reserva que segura lugar impede a renovação (ver [testes-instaveis.md](testes-instaveis.md)).

Testes novos com PostgreSQL real (`ReservaConcorrenciaIntegracaoTest`):
- retorno do provedor durante a expiração em lote;
- cancelamento da viagem durante a expiração, nas duas ordens;
- provedor lento não segura a viagem;
- pagamento iniciado duas vezes ao mesmo tempo gera uma cobrança só.

Unitários: `ProcessadorReembolsosTest` e `ClienteProvedorPagamentoTest`.

### Rodada 3 do QA (24/11/2026): **Aprovado com ressalvas**

Validação dos itens N2, N7, D5, D8, D9, das lacunas de teste e da correção do cancelamento da viagem: todos OK. Na primeira passada, o QA apontou duas ressalvas médias, corrigidas e reconfirmadas na API viva:

| # | Ressalva | Severidade | Situação |
|---|---|---|---|
| R1 | A limpeza de dados E2E apagava qualquer viagem cancelada fora da seed, inclusive de operação real (e a rotina recriaria a viagem) | Média | Corrigido: só sai o que tem a marca. Os testes que criam viagem pela API usam a van `VAN_E2E` (criada no `global-setup`, apagada no teardown). Teste `viagemCanceladaSemAMarcaFica` |
| R2 | Id de viagem com mais de 36 caracteres na lista de passageiros dava erro de banco com SQL na resposta; id inexistente gerava auditoria | Baixa/média | Corrigido: 404 antes de auditar |
| R3 | Reembolsos do cancelamento da viagem rodam em série na própria requisição (até 5 s cada) | Baixa | Aberta: rever com o provedor real (`@Async` ou só agendador) |
| R4 | Cobrança criada enquanto a reserva vence fica aberta no provedor | Baixa | Aberta: cancelar a cobrança no provedor real |
| R5 | Mapa de idempotência do provedor simulado cresce sem limite | Baixa | Aberta (só simulado) |
| R6 | Perfil padrão `dev` expõe o endpoint de limpeza se `SPRING_PROFILES_ACTIVE` faltar | Baixa | Aberta |
| R7 | `DELETE /api/dev/dados-teste` sem `marca` responde ProblemDetail, não `ResponseError` | Baixa | Aberta |
| R8 | E2E do fuso só distingue os fusos a partir das 07:00 no fuso da operação | Baixa | Aberta |
| R10 | `SPACEBUS_PAGAMENTO_TIMEOUT_MS` e `SPACEBUS_REEMBOLSO_MAX_TENTATIVAS` fora do README e do `compose.yaml` | Baixa | Aberta (têm padrão; estão no `.env.example`) |
| R11 | Duas tentativas simultâneas de reembolso que falham contam duas tentativas | Baixa | Aberta |
| R12 | `global-setup` e `idDaVanE2E` procuram a van em até 500 veículos | Baixa | Aberta |

Testes finais: backend 257 (10 + 5 + 3 de integração com PostgreSQL real); E2E 163 passaram, 1 pulado (gaveta, só no celular), 0 falharam.

Testes finais da rodada 2: backend 222 (inclui 5 de integração com PostgreSQL real); E2E 161 passaram e 0 falharam.

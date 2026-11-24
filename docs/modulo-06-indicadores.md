# Módulo 6 — Indicadores básicos

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.6 (o `.docx` prevalece): viagens publicadas, reservadas e canceladas; lugares vendidos e disponibilidade por viagem; ocupação por viagem/horário; comparação simples entre horários da rota. Requisito de aceite do MVP atendido: a transportadora consulta reservas e ocupação por viagem no painel básico. Objetivo deste módulo: **o necessário para a apresentação do MVP**.

Situação: **entregue para teste; Aprovado com ressalvas pelo QA (24/11/2026)**.

## Premissas adotadas

| Tema | Premissa | Onde mudar |
|---|---|---|
| Período | Por **data da partida**, dias no fuso da operação (`APP_FUSO_HORARIO`). Padrão: **hoje − 30 dias até hoje + 7 dias** (mostra o realizado e o que já está vendido). Máximo de **92 dias** | `IndicadorService.PERIODO_MAX_DIAS` |
| Viagens consideradas | Todas as situações, **exceto `RASCUNHO`** (não foi oferecida) | — |
| Lugares vendidos | Soma de `LUGARES` das reservas **`CONFIRMADA`**. Aguardando pagamento, expirada e cancelada não contam | — |
| Lugares oferecidos | Soma da capacidade das viagens **não canceladas** | — |
| Ocupação | Vendidos ÷ oferecidos (média ponderada, não média de percentuais). Sem lugares oferecidos → "—" | — |
| Disponíveis por viagem | Mesma regra de `IReservaService.lugaresDisponiveis` (capacidade − confirmadas − bloqueios vigentes), mas numa consulta agregada | — |
| Trecho | Uma reserva ocupa o lugar na viagem inteira, como na regra de vagas do módulo 3 | — |
| Receita | Soma de `VALOR_TOTAL` das reservas confirmadas, com `SimulatedBadge` (pagamento simulado) | — |
| Cálculo | Sob demanda, por consultas agregadas. Sem tabela nova, cache ou exportação. Uma consulta por bloco (resumo, por horário, por viagem), sem N+1 | — |

O painel não recomenda quantas vans adicionar (escopo 4.6).

## Histórias e critérios de aceite (PO)

### H21 — Resumo e ocupação por horário

Como operador, quero ver os totais do período e a ocupação de cada horário de partida, para comparar os horários da rota.

- [x] Tela **Indicadores** (`/operacao/indicadores`, menu Operação depois de Viagens), para Administrador e Operador. Motorista e Passageiro → 403 na API e rota negada na tela.
- [x] Filtros em `BarraFiltros`: **De**, **Até** (`FiltroData`) e **Rota** (`FiltroSelect`, opcional), com estado na URL (`useFiltrosUrl`). Sem filtro, vale o período padrão. "Limpar filtros" volta ao padrão.
- [x] Validação no servidor: De depois de Até → "A data inicial deve ser anterior ou igual à final."; mais de 92 dias → "O período pode ter no máximo 92 dias."; data ausente ou inválida → período padrão. Rota inexistente → "Rota não encontrada."
- [x] Cartões: **Viagens** (total, com publicadas, realizadas = em andamento + encerradas, canceladas e com reserva = ao menos uma confirmada), **Lugares vendidos** (de oferecidos), **Ocupação** (%) e **Receita** (R$, "Simulado").
- [x] Tabela **Ocupação por horário**: uma linha por horário de partida (HH:mm), em ordem, com viagens, oferecidos, vendidos e ocupação (% com barra proporcional, sem biblioteca de gráfico). Canceladas não entram na ocupação.
- [x] Exemplo verificável: no período, 07:30 com 2 viagens de 15 lugares e 12 vendidos → "07:30 · 2 · 30 · 12 · 40%". Reserva aguardando pagamento ou cancelada no mesmo horário não muda o número.
- [x] Período sem viagens → cartões zerados, ocupação "—" e `SemResultado` "Nenhuma viagem no período." com Limpar filtros.
- [x] Estados: carregando (Skeleton), erro com "Tentar novamente".

### H22 — Ocupação por viagem

Como operador, quero ver lugares vendidos e disponíveis de cada viagem do período, para achar viagens vazias ou lotadas e abrir o cadastro.

- [x] Tabela **Por viagem**, paginada (20), partida mais recente primeiro: partida (dia e hora), rota, veículo, situação (`StatusBadge`), capacidade, vendidos, disponíveis e ocupação %.
- [x] Viagem cancelada: disponíveis e ocupação "—". Viagem lotada: disponíveis 0 e 100%.
- [x] Linha clicável (`LinhaClicavel`) abre `/operacao/viagens/:id` levando a origem; Voltar retorna aos indicadores com os filtros.
- [x] Mesmos filtros de H21; o número de viagens da tabela bate com o cartão Viagens.

## API

| Método e rota | Perfil | Faz |
|---|---|---|
| `GET /api/indicador?inicio=AAAA-MM-DD&fim=AAAA-MM-DD&rotaId=` | Operação | Resumo + ocupação por horário |
| `GET /api/indicador/viagem?inicio=&fim=&rotaId=&page=` | Operação | Ocupação por viagem (`Page`) |

Somente leitura (`BaseSearchController` não se aplica; controller próprio). Restrição por `requestMatchers("/api/indicador/**")` em `SecurityConfiguration` (`OPERACAO`). Os parâmetros são datas e um id; nenhum `*Request` novo com texto livre.

## Fora deste módulo

- Exportação (CSV/PDF), gráficos de série temporal, comparação entre períodos, cache ou tabelas de agregação.
- Indicadores de localização (qualidade do sinal já está no diagnóstico da viagem, módulo 4) e de notificações.
- Métricas de conversão e de qualidade dos dados da seção 8 do escopo.
- Recomendação de frota ou de horários.

## Plano de teste (QA)

| Camada | O que cobre |
|---|---|
| Serviço / repositório | Contagem por situação (rascunho fora); vendidos só confirmadas; oferecidos sem canceladas; ocupação ponderada e "—" sem oferecidos; disponíveis com bloqueio vigente e vencido; agrupamento por HH:mm; limites do período (partida às 00:00 do De e 23:59 do Até entram, fora não); filtro de rota; validação (De > Até, 93 dias, rota inexistente); período padrão com `Clock.fixed` |
| Integração (Testcontainers) | Consultas agregadas no PostgreSQL real com dados conhecidos (o exemplo 07:30 de H21); número de consultas por chamada constante (sem N+1) |
| Controller (`@WebMvcTest`) | 401 sem sessão; 403 para Motorista e Passageiro; 400 com mensagem em pt-BR; formato do JSON |
| E2E (desktop e mobile) | Operador cria viagem de teste (prefixo `E2E`, partida aleatória) e uma reserva confirmada pela API → cartões e linha da viagem mostram os números; filtro de período e rota na URL sobrevivem ao recarregar; linha abre a viagem e Voltar mantém os filtros; período vazio; erro 500 (`page.route`); menu e rota negados ao Motorista e ao Passageiro; estrutura HTML5 (um `h1`, um `main`, rótulos dos filtros). Os campos de data entram em `e2e/limites.spec.ts` se tiverem `min`/`max` |

## Validação

### Rodada 1 do QA (24/11/2026): **Aprovado com ressalvas**

- Backend: 407 testes, 0 falhas. `IndicadorIntegracaoTest` (PostgreSQL real): exemplo 07:30 · 2 · 30 · 12 · 40%, viagem lotada 100%, cancelada fora da ocupação, rascunho fora, limites 00:00/23:59, bloqueio vencido, sem N+1.
- E2E inteiro: 220 passaram, 1 pulado, 1 falhou (`localizacao.spec`, instável, corrigido; ver [testes-instaveis.md](testes-instaveis.md)). `indicadores.spec.ts` 12/12; `limites.spec.ts` inclui a tela.
- Teste manual pela API, conforme: números conferidos com SQL próprio, 401/403, limite de 92 dias, início depois do fim, injeção em `rotaId` → 400.

| # | Defeito | Severidade | Situação |
|---|---|---|---|
| D1 | Ano extremo na data respondia 500 | Baixa | Corrigido |
| D2 | Só uma das datas informada dava erro | Baixa | Corrigido: a outra data é calculada a partir dela |
| D3 | Teste instável de localização sem registro | Baixa | Corrigido (registrado) |

Observação em aberto (backlog): a página da tabela Por viagem não fica na URL; sem risco prático.

Desvios aceitos:

| Desvio | Motivo |
|---|---|
| Prop `titulo` no `SemResultado` | Texto "Nenhuma viagem no período." |
| Coluna Rota oculta no mobile | A rota aparece sob a partida |
| Teste de N+1: mesmo número de consultas com 1 e 7 viagens, no máximo 6 | Prova que não cresce com o volume |
| Período e rota lidos como texto no serviço, sem `*Request` | Validação e mensagens no serviço (D1, D2) |
| E2E confere os números pela linha da viagem do teste | Totais do período variam com outros testes em paralelo |

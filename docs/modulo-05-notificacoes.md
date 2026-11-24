# Módulo 5 — Notificações

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.5 (o `.docx` prevalece). Requisitos de aceite do MVP atendidos: 8 (notificações para quem consentiu; sem WhatsApp, a confirmação continua na aplicação) e a parte de notificação do 10 (falha de entrega identificável). Objetivo deste módulo: **o necessário para a apresentação do MVP**.

Situação: **entregue para teste; Aprovado com ressalvas pelo QA (24/11/2026)**.

## Premissas adotadas (decisões pendentes do escopo, seção 9)

| Decisão pendente | Premissa | Onde mudar |
|---|---|---|
| Canal de notificação | **Na aplicação** (sino no cabeçalho + tela Notificações), sempre, para o passageiro. É o canal que vale | — |
| E-mail e WhatsApp | **Simulados**: `CanalNotificacao` com implementação `CanalNotificacaoSimulado`, que só registra o envio (`ENVIADA`/`FALHOU`). Tela mostra `SimulatedBadge`. Sem integração real | trocar a implementação de `CanalNotificacao` |
| Consentimento | E-mail transacional vai para o e-mail do cadastro. WhatsApp **só com opt-in** do passageiro (preferência com data do consentimento). Sem opt-in, nenhum envio por WhatsApp é registrado | — |
| Falhas de envio | **Visíveis só no log e no item do passageiro**, sem tela para a equipe (decisão do usuário) | — |
| Telefone para WhatsApp | **Não coletado** no MVP (envio simulado não precisa). Pendente para a integração real | — |
| Lembrete | **1 h antes** da partida, por tarefa agendada (a cada minuto) | `spacebus.notificacao.lembrete-minutos` |
| Tentativas | **Uma** tentativa por canal externo (simulado). Retry fica para a integração real | — |
| Falha simulada | Desligada; ligada só em teste | `spacebus.notificacao.simular-falha` |

Fora de cogitação no MVP: templates editáveis, fila externa, push do navegador, SMS, painel administrativo de envios.

## Eventos e destinatários

| Tipo (`TipoNotificacao`) | Disparo | Quem recebe | Título / texto (exemplo) | Chave única |
|---|---|---|---|---|
| `RESERVA_CONFIRMADA` | Retorno aprovado do provedor confirma a reserva | Dono da reserva | "Reserva AB12CD confirmada" / "Centro → Aeroporto · 25/11 07:30 · 2 lugares." | `CONFIRMADA:{reservaId}` |
| `VIAGEM_ALTERADA` | Edição da viagem muda a **partida** | Reservas `AGUARDANDO_PAGAMENTO` e `CONFIRMADA` | "Horário alterado: reserva AB12CD" / "Nova partida: 25/11 08:00 (antes 07:30)." | `ALTERADA:{reservaId}:{novaPartida}` |
| `VIAGEM_CANCELADA` | `ViagemService.cancelar` cancela a reserva (`MotivoCancelamento.VIAGEM_CANCELADA`) | Reservas canceladas por isso | "Viagem cancelada: reserva AB12CD" / "O valor será reembolsado." (só se havia pagamento aprovado; senão "A reserva foi cancelada.") | `CANCELADA:{reservaId}` |
| `VIAGEM_INICIADA` | Viagem passa a `EM_ANDAMENTO` (motorista ou operação) | Reservas `CONFIRMADA` | "A van saiu" / "Acompanhe a localização." | `INICIADA:{reservaId}` |
| `LEMBRETE` | Tarefa agendada: partida nos próximos 60 min, viagem `PUBLICADA` | Reservas `CONFIRMADA` | "Sua viagem parte às 07:30" / "Centro → Aeroporto · 07:30 · embarque em Praça Central." | `LEMBRETE:{reservaId}` |

Toda notificação tem link para a reserva (`/reservas/{id}`).

## Histórias e critérios de aceite (PO)

### H18 — Gerar notificações sem afetar a reserva

Como passageiro, quero ser avisado do que acontece com a minha reserva, para não perder a viagem.

- [x] Cada evento da tabela gera **uma** notificação por reserva atingida, com tipo, título, texto, link e hora.
- [x] Geração **depois do commit** (`@TransactionalEventListener(AFTER_COMMIT)`, transação nova). Se a transação de origem falha (ex.: cancelamento da viagem rejeitado), nada é gerado.
- [x] Erro ao gerar ou enviar (banco, canal) é registrado em log e **não muda** reserva, pagamento, viagem nem a resposta da operação de origem.
- [x] Sem duplicar: índice único em `CHAVE`. Webhook repetido, dois inícios simultâneos ou duas execuções da tarefa de lembrete → uma notificação. Conflito na chave é ignorado sem erro.
- [x] Editar a viagem sem mudar a partida (preço, veículo) → nenhuma notificação. Mudar a partida duas vezes → duas notificações (uma por novo horário).
- [x] Viagem sem reservas ativas → nada é gerado em nenhum evento.
- [x] Lembrete: reserva confirmada com partida em 59 min → gera; em 61 min → não gera ainda; viagem suspensa, cancelada ou em andamento → não gera; partida já passou → não gera. Usa `Clock`.
- [x] Reserva confirmada já dentro da janela de 1 h recebe confirmação e lembrete (aceito no MVP).

### H19 — Ver notificações na aplicação

Como passageiro, quero ver minhas notificações e quais ainda não li, para saber o que mudou sem procurar.

- [x] Sino no cabeçalho, **só para o perfil Passageiro**, com a contagem de não lidas (oculta quando zero; "9+" acima de 9). Rótulo acessível: "Notificações, 3 não lidas". Atualiza a cada 30 s e ao voltar para a aba.
- [x] Sino leva a `/notificacoes` (item **Notificações** no grupo Passageiro do menu). Lista paginada, mais recentes primeiro; não lidas destacadas. Cada item: título, texto, hora relativa ao dia ("hoje 07:12", "24/11 18:40") e os envios externos (ver H20).
- [x] Abrir um item marca como lida e leva à reserva. "Marcar todas como lidas" no topo (só com não lidas). Contagem do sino acompanha.
- [x] Notificação de outro passageiro → 404 / "Notificação não encontrada." (não revela existência).
- [x] Estados: carregando (Skeleton), vazio "Nenhuma notificação", erro com "Tentar novamente".
- [x] Da reserva para as notificações e de volta: o link do item abre `/reservas/:id`; Voltar retorna à lista.

### H20 — E-mail e WhatsApp simulados com consentimento

Como passageiro, quero escolher receber também por WhatsApp, para ser avisado fora da aplicação só se eu autorizar.

- [x] Na tela Notificações, opção "Receber por WhatsApp" (switch, desligado por padrão) com `SimulatedBadge`. Ligar grava a data do consentimento; desligar apaga. Vale para as próximas notificações.
- [x] Cada notificação gera um envio `EMAIL` e, com opt-in, um envio `WHATSAPP`, pelo `CanalNotificacao`. Cada envio guarda canal, situação (`ENVIADA`/`FALHOU`), hora e motivo da falha (texto curto, sem dados pessoais).
- [x] Sem opt-in → nenhum envio de WhatsApp registrado.
- [x] Canal falha (`simular-falha` ligado ou exceção) → envio `FALHOU`; a notificação continua na aplicação; a reserva não muda.
- [x] Na lista, cada item mostra "E-mail · Enviado" / "WhatsApp · Falhou" com `StatusBadge` e `SimulatedBadge`.
- [x] Falha de envio vai para o log com id da notificação e canal, sem e-mail ou telefone (requisito 10).

## Regras

- Destinatário é sempre o passageiro da reserva; operador, motorista e administrador não recebem nem veem notificações de passageiro no MVP.
- Listener nunca segura trava de viagem/reserva; lê os dados necessários numa transação nova. Envio pelo canal fora de transação.
- Texto montado no servidor (pt-BR), com data/hora no fuso da aplicação. Sem template editável.
- Limites: `TITULO VARCHAR(120)`, `MENSAGEM VARCHAR(300)`, `LINK VARCHAR(200)`, `CHAVE VARCHAR(120)`, `ENVIO.MOTIVO VARCHAR(200)`. Preferência só tem booleano (`LimitesDosRequestsTest` continua passando).

## Banco

- `NOTIFICACAO`: id, `USUARIO_ID`, `RESERVA_ID`, `TIPO`, `TITULO`, `MENSAGEM`, `LINK`, `CHAVE` (única), `LIDA_EM`, datas. Índice (`USUARIO_ID`, `LIDA_EM`).
- `NOTIFICACAO_ENVIO`: id, `NOTIFICACAO_ID`, `CANAL` (`EMAIL`, `WHATSAPP`), `STATUS` (`ENVIADA`, `FALHOU`), `MOTIVO`, datas. Única (`NOTIFICACAO_ID`, `CANAL`).
- `USUARIO.WHATSAPP_CONSENTIDO_EM` (nulo = sem consentimento).

## API

| Método e rota | Perfil | Faz |
|---|---|---|
| `GET /api/notificacao` | Passageiro | Minhas notificações, paginadas, com envios |
| `GET /api/notificacao/nao-lidas` | Passageiro | `{ quantidade }` |
| `POST /api/notificacao/{id}/ler` | Passageiro dono | Marca como lida (idempotente) |
| `POST /api/notificacao/ler-todas` | Passageiro | Marca todas como lidas |
| `GET`, `PUT /api/notificacao/preferencias` | Passageiro | `{ whatsapp: boolean, whatsappConsentidoEm }` |

Restrição por `requestMatchers("/api/notificacao/**").hasRole("PASSAGEIRO")` em `SecurityConfiguration`.

## Telas

| Tela | Perfil | Chega de → vai para |
|---|---|---|
| Sino no `Header` | Passageiro | Qualquer tela → `/notificacoes` |
| `/notificacoes` (menu Passageiro, depois de Minhas reservas) | Passageiro | Sino ou menu ↔ `/reservas/:id` |

## Fora deste módulo

- Integração real de e-mail/WhatsApp, coleta e verificação de telefone, retry e custos de envio (pendente: decisão do canal, escopo seção 9).
- Push do navegador, SMS, templates editáveis, fila externa, painel de envios para a equipe.
- Notificação ao operador ou ao motorista; aviso de suspensão de viagem; aviso de reembolso concluído; expiração da reserva.
- Notificação perdida se a aplicação cair entre o commit e o listener (sem outbox no MVP; o lembrete não é afetado, vem da tarefa agendada).

## Plano de teste (QA)

| Camada | O que cobre |
|---|---|
| Serviço | Cada evento gera o tipo, texto e destinatários certos; edição sem mudar partida não gera; duas mudanças geram duas; viagem sem reservas não gera; chave repetida ignorada; lembrete com `Clock.fixed` nos limites (59/61 min, suspensa, cancelada, em andamento, passada); opt-in gera envio WhatsApp e sem opt-in não; falha do canal grava `FALHOU` e não propaga; marcar lida/todas; notificação de outro passageiro |
| Evento | Rollback da transação de origem não gera notificação; exceção no listener não altera a reserva nem a resposta do webhook/cancelamento |
| Controller (`@WebMvcTest`) | 401 sem sessão; 403 para Operador, Motorista e Administrador em `/api/notificacao/**`; 404 de notificação alheia; formato do JSON (sem e-mail no envio) |
| Integração (Testcontainers) | Webhook de aprovação repetido em paralelo → uma notificação `RESERVA_CONFIRMADA`; duas execuções simultâneas do lembrete → uma por reserva |
| E2E (desktop e mobile) | Passageiro reserva e paga (simulado) → sino mostra +1, lista mostra "Reserva … confirmada" com envio e "Simulado"; abrir marca lida e leva à reserva; marcar todas; operador altera a partida e cancela uma viagem de teste (pela API) → passageiro vê as notificações; switch WhatsApp liga e o próximo envio mostra WhatsApp; vazio, erro 500 (`page.route`); sino ausente para Operador e Motorista e `/notificacoes` negada; estrutura HTML5 (um `h1`, um `main`, rótulo do sino) |

## Validação

### Rodada 1 do QA (24/11/2026): **Aprovado com ressalvas**

- Backend: 389 testes, 0 falhas (inclui `NotificacaoIntegracaoTest` com PostgreSQL real).
- E2E inteiro: 207 passaram, 1 pulado, 0 falharam; `notificacoes.spec.ts` 14/14.
- Teste manual pela API, todo conforme: confirmação sem duplicar, só mudança de partida notifica, WhatsApp com opt-in, cancelamento com reembolso, lembrete único, "A van saiu", autorização (401, 403, 404).

| # | Ressalva | Severidade | Situação |
|---|---|---|---|
| R1 | Texto do lembrete "parte em 1 hora" | Baixa | Corrigido: "parte às HH:mm" |
| R2 | Limites do lembrete (59/61 min, situações) testados só com mock | Baixa | Aberto (backlog) |
| R3 | Sem teste automatizado do 404 para notificação real de outro passageiro | Baixa | Aberto (backlog); verificado manualmente |
| R4 | Erro num envio interrompia os envios seguintes | Baixa | Corrigido |
| R5 | Paginação com `EntityGraph` de coleção pagina em memória | Baixa | Aberto (backlog; desempenho) |

Desvios aceitos:

| Desvio | Motivo |
|---|---|
| `POST /api/dev/dados-teste/passageiro?marca=E2E` | Um passageiro por teste E2E; a limpeza da marca o apaga |
| `e2e/limites.spec.ts` sem alteração | A tela nova só tem o switch, sem campo de texto ou número |
| Texto da notificação pelo trecho da reserva | Mostra o embarque e o desembarque do passageiro, não a rota inteira |
| "A van saiu" sem o código da reserva | Texto curto; o link leva à reserva |
| Switch do shadcn escrito à mão | Mesmo visual e acessibilidade do componente |
| Falha simulada sem E2E | Coberta por teste de serviço (`simular-falha`) |
| Falhas de envio só no log e no item do passageiro | Decisão do usuário para a apresentação do MVP |

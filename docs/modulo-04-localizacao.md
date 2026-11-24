# Módulo 4 — Localização da viagem

Escopo: [escopo-mvp.md](escopo-mvp.md), seção 4.2, seção 4.4 ("sinalizar viagem iniciada, concluída") e Fluxo A passo 7 / Fluxo B passo 4. Requisitos de aceite do MVP atendidos: 5 (mapa mostra última posição e horário; dado indisponível é informado) e a parte de localização do 10 (equipe identifica falhas sem expor dados sensíveis). Métrica de referência (seção 8): posição atualizada dentro de 60 s quando houver cobertura.

Situação: **entregue para teste; Aprovado com ressalvas pelo QA (24/11/2026)**.

## Premissas adotadas (decisões pendentes do escopo, seção 9)

| Decisão pendente | Premissa | Onde mudar |
|---|---|---|
| Fonte da localização | **Celular do motorista**, pela Geolocation API do navegador (PWA). GPS embarcado fica fora | — |
| Quando ligar/desligar | Liga ao **iniciar** a viagem, desliga ao **encerrar**. Iniciar abre **30 min antes** da partida e fecha **2 h depois** dela | `spacebus.localizacao.inicio-antes-min`, `inicio-depois-min` |
| Celular durante a viagem | Página aberta e **tela ligada** (Wake Lock quando o navegador tiver), aparelho no suporte. Com a tela bloqueada o navegador pode parar de enviar; isso aparece como "Desatualizada", nunca escondido. **Premissa adotada sem confirmação do usuário** (alternativas: app nativo, fora do escopo, ou GPS da transportadora) | — |
| Encerramento esquecido | Viagem em andamento é encerrada automaticamente **6 h após o início** | `spacebus.localizacao.encerramento-automatico-horas` |
| Privacidade e retenção | Guarda **só a última posição** da viagem, sem trajeto. As coordenadas são apagadas **24 h após o encerramento**; ficam horários e contagens (qualidade do sinal). Passageiro só vê a posição com a viagem em andamento. **Premissa adotada sem confirmação do usuário**; rever com a política de privacidade | `spacebus.localizacao.retencao-horas` |
| Dado desatualizado | Última posição com mais de **2 min** é "Desatualizada" (continua no mapa, com a hora). Intervalo acima de **60 s** entre posições conta como lacuna | `desatualizada-segundos`, `lacuna-segundos` |
| Viagem interrompida | Viagem em andamento **não pode ser cancelada** no MVP; o operador encerra. Política de reembolso de viagem interrompida fica pendente | `ViagemService.cancelar` |
| Mapa | **Leaflet** (`react-leaflet`) com tiles do **OpenStreetMap**, com atribuição. Volume do piloto cabe na política de uso do OSM; em produção, trocar o provedor de tiles | `features/localizacao/mapa.ts` |
| ETA | Fora do módulo (escopo 4.2 e seção 6) | — |

Confirmar com a transportadora antes do piloto, junto com a política de privacidade.

## Ciclo da viagem

```
RASCUNHO → PUBLICADA ⇄ SUSPENSA
              │
              └─ iniciar ─► EM_ANDAMENTO ─ encerrar ─► ENCERRADA (definitivo)
RASCUNHO, PUBLICADA, SUSPENSA → CANCELADA (definitivo)
```

| Situação | Tela | Tom | Busca | Reserva nova | Edição |
|---|---|---|---|---|---|
| `EM_ANDAMENTO` | Em andamento | `andamento` | Não aparece | Não | Somente leitura |
| `ENCERRADA` | Encerrada | `info` | Não aparece | Não | Somente leitura |

Efeito sobre reservas:

- Confirmadas continuam confirmadas (não há "embarcou" no MVP).
- Aguardando pagamento expiram no prazo normal. Aprovação que chega depois do início não confirma: expira e reembolsa (regra já existente: só confirma viagem `PUBLICADA` que ainda vai partir).
- Cancelamento pelo passageiro já termina 2 h antes da partida; nada muda.

Mudanças no banco: `VIAGEM.STATUS` passa de `VARCHAR(10)` para `VARCHAR(12)` e o `ck_viagem_status` inclui os dois valores. Colunas novas na viagem: `INICIADA_EM`, `INICIADA_POR`, `ENCERRADA_EM`, `ENCERRADA_POR` (nulo quando automático). Índices únicos parciais: um motorista e um veículo têm no máximo **uma** viagem `EM_ANDAMENTO` (`WHERE status = 'EM_ANDAMENTO'`). A renovação da seed (`20261123-1-renova-seed-viagem`) não move viagem em andamento ou encerrada.

## Histórias e critérios de aceite (PO)

### H13 — Iniciar e encerrar a viagem

Como motorista, quero iniciar e encerrar a viagem com um toque, para ligar e desligar a localização sem mexer no celular enquanto dirijo.

- [x] Viagem `PUBLICADA` → Iniciar → `EM_ANDAMENTO`, com hora e responsável gravados. Só a viagem do motorista atribuído aparece para ele; operador e administrador também iniciam e encerram (sem enviar posição).
- [x] Iniciar 31 min antes da partida → "A viagem pode ser iniciada a partir de 30 minutos antes da partida." Exatamente 30 min antes → inicia (limite inclusivo). Mais de 2 h depois da partida → "O prazo para iniciar esta viagem acabou."
- [x] Viagem em rascunho, suspensa, cancelada ou encerrada → "Só viagens publicadas podem ser iniciadas."
- [x] Motorista de outra viagem → 403 / "Só o motorista da viagem pode iniciar." Motorista com outra viagem em andamento → "Encerre a viagem em andamento antes de iniciar outra." Veículo em outra viagem em andamento → "O veículo já está em outra viagem em andamento."
- [x] Dois toques simultâneos em Iniciar (ou motorista e operador ao mesmo tempo) → uma transição; o segundo recebe "A viagem já está em andamento." Dois inícios simultâneos do mesmo motorista em viagens diferentes → só um passa (índice único no banco).
- [x] Encerrar `EM_ANDAMENTO` → `ENCERRADA`; posições que chegam depois são recusadas. Encerrar outra situação → "Só viagens em andamento podem ser encerradas."
- [x] Cancelar viagem em andamento → "Viagem em andamento não pode ser cancelada. Encerre a viagem."
- [x] Viagem esquecida em andamento é encerrada pela tarefa agendada 6 h após o início (`SKIP LOCKED`; não briga com o encerramento manual).
- [x] Viagem em andamento some da busca e do detalhe do passageiro, e não aceita reserva ("Esta viagem não está mais disponível.").

### H14 — Transmitir a localização

Como motorista, quero que o celular envie a posição sozinho durante a viagem, para os passageiros acompanharem a van.

- [x] Tela **Minhas viagens** (`/motorista/viagens`, mobile primeiro): viagens de hoje atribuídas a mim (fuso da operação), com rota, partida, veículo e situação. Botão Iniciar habilitado só na janela; fora dela, o motivo em uma linha.
- [x] Ao iniciar, o navegador pede permissão de localização. Transmitindo, a tela mostra em destaque "Transmitindo", a hora da última posição enviada e o botão Encerrar (com confirmação). Nenhuma outra ação é necessária até encerrar.
- [x] Envio: `watchPosition` com alta precisão; no máximo uma posição a cada **15 s** (a mais recente). Wake Lock pedido quando disponível; sem ele, nada quebra.
- [x] Permissão negada → "Permita a localização no navegador para transmitir." Sem sinal de GPS → "Sem sinal de GPS". A viagem continua em andamento nos dois casos.
- [x] Sem internet: posições vão para uma fila de até **20**, enviadas em lote ao voltar; a tela mostra "Sem conexão · n na fila". Fila cheia descarta a mais antiga. Posições com mais de 10 min são descartadas antes do envio.
- [x] Recarregar a página com a viagem em andamento retoma a transmissão sem novo Iniciar (depois da permissão do navegador).
- [x] Servidor (`POST /api/motorista/viagem/{id}/localizacao`, lote de 1 a 20):
  - só o motorista atribuído, com a viagem `EM_ANDAMENTO`; senão "A viagem não está em andamento." e o app para de transmitir;
  - validação: latitude −90 a 90, longitude −180 a 180, precisão 0 a 5000 m, hora com fuso (ISO-8601 com offset, convertida para o fuso da aplicação; nunca hora local sem fuso). Mensagens: "Latitude deve estar entre -90 e 90.", "Longitude deve estar entre -180 e 180.", "Precisão deve estar entre 0 e 5000 m.", "Informe a hora da posição.", "Envie de 1 a 20 posições.";
  - **rejeitadas** (contadas, com motivo; o lote não falha): hora mais de 2 min à frente do servidor (aparelho adiantado), mais de 10 min atrás (antiga), precisão acima de 200 m (imprecisa);
  - **ignoradas** (sem ser falha): mais antiga que a última gravada (fora de ordem) ou a menos de 10 s da última (frequente);
  - resposta: `{ aceitas, rejeitadas, ignoradas }`.
- [x] **Posição mais antiga nunca sobrescreve uma mais nova**, mesmo com requisições simultâneas (gravação condicional no banco, não comparação em memória). Posição concorrente com o encerramento não é gravada depois dele.

### H15 — Acompanhar a van

Como passageiro com reserva confirmada, quero ver onde a van está, para sair na hora certa para o ponto de embarque.

- [x] Na tela da reserva (`/reservas/:id`), reserva `CONFIRMADA` com a viagem em andamento: bloco **Localização da van** com mapa, marcador na última posição, "Atualizada às HH:MM" e a precisão ("± 30 m"). Atualiza a cada **15 s** (polling; sem WebSocket).
- [x] Mais de 2 min sem posição: selo "Desatualizada" (`atencao`) e "Última posição às HH:MM"; o marcador continua, esmaecido. Nunca some sem aviso.
- [x] Viagem em andamento sem nenhuma posição: "Localização indisponível", sem mapa e sem posição inventada.
- [x] Viagem ainda não iniciada (a partir de 30 min antes): "A localização aparece quando a viagem começar." Encerrada: "Viagem encerrada", sem posição. Fora disso, o bloco não aparece.
- [x] Desatualizada é decidida pelo servidor (`desatualizada`, `segundosDesdeAtualizacao`), não pelo relógio do celular do passageiro.
- [x] Em Minhas reservas, reserva com viagem em andamento mostra "Em andamento" e o link "Acompanhar", que abre a reserva no bloco de localização.
- [x] Reserva de outro passageiro → 404. Reserva aguardando pagamento, expirada ou cancelada → sem localização (`GET` responde "Localização disponível só para reservas confirmadas.").
- [x] Tiles do mapa sem carregar (sem internet, provedor fora): hora, situação e aviso continuam na tela, fora do mapa.
- [x] Falha da API na atualização: mantém a última leitura com "Não foi possível atualizar. Tentando de novo." e segue tentando.

### H16 — Localização na operação

Como operador, quero ver a posição e a qualidade do sinal das viagens em andamento, para agir quando a localização falhar.

- [x] No bloco Situação da edição da viagem: Iniciar (viagem publicada, na janela) e Encerrar (em andamento), com confirmação, no padrão de `viagem-acoes.tsx`.
- [x] Viagem em andamento ou encerrada: bloco **Localização** com o mesmo mapa e avisos do passageiro, mais o diagnóstico: início, última posição recebida, aceitas, rejeitadas por motivo (aparelho adiantado, antiga, imprecisa), lacunas acima de 60 s e maior intervalo.
- [x] Na listagem de viagens, viagem em andamento com localização desatualizada ou indisponível mostra o selo "Sem sinal". Filtro de situação inclui Em andamento e Encerrada.
- [x] Viagem em andamento sem motorista atribuído: "Sem motorista: localização indisponível".
- [x] Falhas sem dados sensíveis: logs de rejeição com viagem, motivo e contagem, sem coordenadas, nome ou e-mail. Consulta do Administrador grava `auditoria_acesso` no máximo uma vez por usuário, viagem e hora (não a cada atualização).

### H17 — Localização simulada (demonstração e testes)

Como equipe Space Bus, quero simular posições, para demonstrar e testar sem uma van na rua.

- [x] `POST /api/dev/localizacao/{viagemId}` (perfil `dev`, só Administrador) grava uma posição na viagem em andamento pelo mesmo caminho do motorista, marcada como simulada.
- [x] Posição simulada aparece com `SimulatedBadge` ("Simulado") para passageiro e operador. Fora do perfil `dev`, o endpoint não existe.

## Regras

- Ordem de travas: **viagem → localização**. Iniciar, encerrar e o encerramento automático travam a viagem (`ViagemRepository.travar`). O envio de posição trava a viagem em modo compartilhado (`FOR SHARE`), confere situação e motorista e grava com `UPDATE ... WHERE registrada_em IS NULL OR registrada_em < :nova`. Assim posições não bloqueiam umas às outras e não passam do encerramento.
- Tabela `LOCALIZACAO_VIAGEM`, uma linha por viagem: latitude, longitude, precisão, `REGISTRADA_EM` (aparelho), `RECEBIDA_EM` (servidor), `SIMULADA`, contadores (aceitas, rejeitadas por motivo, ignoradas, lacunas, maior intervalo), último motivo de rejeição e hora. Sem histórico de trajeto.
- Expurgo: tarefa agendada apaga latitude/longitude 24 h após o encerramento (`SKIP LOCKED`); contadores e horários ficam para o módulo 6.
- "Agora" sempre por `Clock`. Toda janela e limite em `spacebus.localizacao.*`, com o mesmo valor em `features/localizacao/limites.ts`, no README e no `.env.example`.
- Autorização no servidor: `/api/motorista/**` só `MOTORISTA`; iniciar/encerrar/localização em `/api/viagem/**` só `OPERACAO`; localização da reserva em `/api/reserva/**` só `PASSAGEIRO` e dono.
- A Geolocation API exige contexto seguro: `localhost` em dev; **HTTPS** obrigatório no ambiente do piloto.

## API

| Método e rota | Perfil | Faz |
|---|---|---|
| `GET /api/motorista/viagem` | Motorista | Minhas viagens de hoje |
| `POST /api/motorista/viagem/{id}/iniciar`, `/encerrar` | Motorista atribuído | Transições |
| `POST /api/motorista/viagem/{id}/localizacao` | Motorista atribuído | Lote de 1 a 20 posições |
| `POST /api/viagem/{id}/iniciar`, `/encerrar` | Operação | Transições pela transportadora |
| `GET /api/viagem/{id}/localizacao` | Operação | Posição + diagnóstico |
| `GET /api/reserva/{id}/localizacao` | Passageiro dono | Posição da viagem da reserva confirmada |
| `POST /api/dev/localizacao/{viagemId}` | Administrador, só `dev` | Posição simulada |

Resposta de localização (passageiro): `situacaoViagem`, `disponivel`, `latitude`, `longitude`, `precisaoM`, `registradaEm`, `segundosDesdeAtualizacao`, `desatualizada`, `simulada`. A da operação acrescenta o diagnóstico. Nenhuma expõe motorista ou passageiros.

## Telas

| Tela | Perfil | Chega de → vai para |
|---|---|---|
| `/motorista/viagens` (novo grupo de menu **Motorista**, item Minhas viagens, primeiro para o perfil Motorista) | Motorista | Tela inicial do motorista; transmissão na própria tela |
| Bloco Localização da van em `/reservas/:id` | Passageiro | Minhas reservas ("Acompanhar") ↔ reserva |
| Bloco Localização e ações Iniciar/Encerrar em `/operacao/viagens/:id` | Operação | Listagem (selo "Sem sinal") ↔ edição da viagem ↔ reservas da viagem |

Componentes novos em `features/localizacao`: `MapaVan` (Leaflet, carregado só onde é usado), `SituacaoLocalizacao` (selo + hora + avisos, fora do mapa), `useTransmissao` (watchPosition, intervalo, fila, Wake Lock), `useLocalizacao` (polling 15 s).

## Fora deste módulo

- ETA, trajeto percorrido, coordenadas dos pontos de parada no mapa (`PontoParada` não tem coordenadas; a rota não é desenhada).
- GPS embarcado ou integração com rastreador da transportadora.
- Transmissão com a tela bloqueada (exige app nativo, fora do escopo).
- Aviso de atraso ao passageiro e notificação de início da viagem (módulo 5).
- Confirmação de embarque do passageiro.
- Cancelamento de viagem em andamento e seu reembolso.

## Plano de teste (QA)

| Camada | O que cobre |
|---|---|
| Serviço | Todas as transições e mensagens de H13 (janela inclusiva nos dois limites, motorista errado, outra viagem em andamento do motorista e do veículo, cancelar em andamento); efeito na busca e na reserva (aprovação do provedor depois do início expira e reembolsa); validação e classificação de posições (aceita, adiantada, antiga, imprecisa, fora de ordem, frequente) com `Clock.fixed`; contadores e lacunas; desatualizada após 2 min; acesso do passageiro (dono, só confirmada, só em andamento); encerramento automático; expurgo após 24 h; simulada marcada |
| Controller (`@WebMvcTest`) | 401 sem sessão e 403 por perfil em cada rota nova (passageiro e operador em `/api/motorista/**`, motorista em `/api/viagem/**` e `/api/reserva/**`); validação do lote (vazio, 21 itens, lat/lon/precisão fora, hora sem fuso); formato do JSON sem dados do motorista; `/api/dev/localizacao` ausente fora do `dev`. `LimitesDosRequestsTest` continua passando |
| Integração (Testcontainers) | Posições simultâneas fora de ordem: fica a de maior hora; posição × encerrar: nada gravado depois do encerramento; dois inícios simultâneos da mesma viagem; dois inícios simultâneos do mesmo motorista em viagens diferentes (índice único); encerramento automático × encerramento manual |
| E2E (desktop e mobile) | Motorista com `context.grantPermissions(['geolocation'])` e `setGeolocation`: inicia, transmite, a posição aparece para o passageiro com a hora, encerra. Permissão negada. Offline com `context.setOffline`: fila e envio ao voltar. Passageiro: indisponível, atualizada, desatualizada (via simulação com hora antiga) e encerrada; link "Acompanhar" e volta. Operador: iniciar/encerrar com confirmação, diagnóstico, selo "Sem sinal" na listagem. Permissões por perfil nas rotas novas. Tiles bloqueados com `page.route` (sem dependência de internet; teste do mapa sem tiles). Estrutura HTML5 da tela do motorista. 500 na atualização simulado com `page.route` |

Dados de teste: cada teste cria a própria viagem com a van `VAN_E2E`, partida aleatória dentro da janela e **o próprio motorista** (marca `E2E`; a limpeza `DELETE /api/dev/dados-teste` passa a apagar também esses usuários e as localizações), porque o motorista só pode ter uma viagem em andamento e desktop e mobile rodam em paralelo. Viagem da seed não é iniciada.

## Validação

### Rodada 1 do QA (24/11/2026): **Aprovado com ressalvas**

Sem defeito alto. Testes: backend 355, 0 falhas (inclui `LocalizacaoConcorrenciaIntegracaoTest`, 8 cenários com PostgreSQL real); E2E 191 passaram, 1 pulado, 0 falharam (`localizacao.spec.ts` 28/28).

Depois das correções de D1–D4: backend 357, 0 falhas (`LocalizacaoConcorrenciaIntegracaoTest` com 9 cenários); `localizacao.spec.ts` 30/30; `tsc`, lint e build sem erros.

Teste manual do QA pela API, todo conforme:
- janela de início, inclusive os dois limites;
- autorização: 401 sem sessão e 403 por perfil nas rotas novas;
- concorrência de início, de posições e de encerramento;
- validação do lote e classificação das posições;
- pagamento aprovado depois do início: reserva `EXPIRADA` e pagamento `REEMBOLSADO`.

| # | Defeito | Severidade | Situação |
|---|---|---|---|
| D1 | Consultas simultâneas do Administrador gravavam auditoria duplicada | Baixa | Corrigido, com teste |
| D2 | Detalhe do histórico enganoso quando a aprovação chega depois do início | Baixa | Corrigido, com teste |
| D3 | A transmissão do motorista parava em qualquer 4xx e não avisava posições recusadas | Baixa | Corrigido, com teste |
| D4 | JSON malformado respondia erro técnico em inglês | Baixa | Corrigido, com teste |

Desvios do plano aceitos pelo QA:

| Desvio | Motivo |
|---|---|
| Reserva em viagem não publicada responde "Esta viagem não está mais disponível." | Mensagem única para o passageiro (texto da H13 ajustado) |
| Uma van e um motorista por teste E2E | Isola testes em paralelo (um motorista e um veículo só têm uma viagem em andamento) |
| `POST /api/dev/dados-teste/motorista?marca=E2E` | Cria o motorista do teste; a limpeza da marca o apaga |
| Hora da posição recebida como texto ISO-8601 com fuso | Convertida no servidor para o fuso da aplicação |
| Viagem encerrada não mostra posição (também na operação) | Privacidade; o diagnóstico continua |
| Prop `topo` no `CadastroForm` | Bloco de localização acima do formulário da viagem |
| Leaflet em `optimizeDeps` do Vite | Evita recarga do Vite no primeiro carregamento do mapa |

Premissas ainda sem confirmação do usuário (adotadas, ver tabela de premissas): tela ligada durante a viagem e retenção de 24 h das coordenadas.

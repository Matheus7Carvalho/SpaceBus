# SpaceBus — Escopo do MVP (resumo de trabalho)

Fonte: [SpaceBus_Escopo_MVP_v0.1.docx](SpaceBus_Escopo_MVP_v0.1.docx) (v0.1, 17/11/2026, proposta para validação). Este resumo orienta o backlog; em caso de dúvida, vale o documento original.

## Objetivo

Testar, em operação limitada, se passageiros conseguem **encontrar, acompanhar, reservar e pagar** uma viagem de van, e se uma transportadora consegue **manter horários, capacidade, disponibilidade e localização atualizados**.

## Limites do piloto (recomendação, a confirmar com a transportadora)

- Uma cidade, uma transportadora, uma rota com poucos horários e pontos de embarque.
- Vans no início; o modelo de dados já aceita ônibus.
- Aplicação web responsiva (PWA), sem app nativo.
- Localização pelo celular do motorista durante a viagem ativa (ou GPS já existente).
- Pagamento por provedor externo; nenhum dado de cartão armazenado.
- WhatsApp só com integração aprovada e consentimento; alternativa: confirmação na aplicação e e-mail/SMS.

## Perfis

| Perfil | No sistema (`Perfil`) | Faz |
|---|---|---|
| Passageiro | `PASSAGEIRO` | Busca por origem/destino/data, vê detalhes e disponibilidade, acompanha a van, reserva e paga |
| Operador da transportadora | `OPERADOR` | Cadastra rota, pontos, horários, veículos e capacidade; publica/altera/cancela viagens; vê reservas |
| Motorista | `MOTORISTA` | Inicia e encerra a transmissão da localização, sem interagir com o aparelho dirigindo |
| Administrador Space Bus | `ADMINISTRADOR` | Apoia o cadastro inicial, configura acessos, resolve problemas; acesso limitado e registrado |

## Módulos e ordem de entrega

| # | Módulo | Escopo (seção do documento) | Situação |
|---|---|---|---|
| 1 | **Cadastro operacional** | 4.4: veículos, rota e pontos, viagens com horário, preço, capacidade; publicar, suspender, cancelar | **Aprovado com ressalvas** ([modulo-01-cadastro-operacional.md](modulo-01-cadastro-operacional.md)) |
| 2 | **Consulta de viagens** (passageiro) | 4.1: busca por origem/destino/data, detalhe da rota, mensagem sem resultado | **Aprovado com ressalvas** ([modulo-02-consulta-viagens.md](modulo-02-consulta-viagens.md)) |
| 3 | **Reserva e pagamento** | 4.3: reserva com código, bloqueio temporário da vaga, provedor externo, sem dupla venda | **Aprovado com ressalvas** ([modulo-03-reserva-pagamento.md](modulo-03-reserva-pagamento.md)) |
| 4 | **Localização da viagem** | 4.2: posição pelo celular do motorista, hora da última atualização, aviso de dado desatualizado | **Aprovado com ressalvas** ([modulo-04-localizacao.md](modulo-04-localizacao.md)) |
| 5 | **Notificações** | 4.5: confirmação, alteração/cancelamento, lembrete; WhatsApp com consentimento | **Aprovado com ressalvas** ([modulo-05-notificacoes.md](modulo-05-notificacoes.md)) |
| 6 | Indicadores básicos | 4.6: viagens, lugares vendidos, ocupação por horário | A fazer |

## Requisitos de aceite do MVP (seção 5, resumidos)

1. Passageiro encontra viagem ativa por origem, destino e data, ou recebe mensagem clara de ausência.
2. Disponibilidade consistente com as reservas; nunca duas vendas da mesma vaga.
3. Pagamento aprovado só após confirmação do provedor; sem cobrança duplicada.
4. Passageiro consulta a reserva depois de sair e voltar.
5. Mapa mostra última posição e horário; dado indisponível é informado.
6. **Operador cadastra e altera horários, capacidade e estado da viagem sem intervenção técnica.** (módulo 1)
7. Alteração ou cancelamento afeta a disponibilidade e identifica os passageiros a avisar.
8. Notificações para quem consentiu; sem WhatsApp, a confirmação continua na aplicação.
9. Transportadora consulta reservas e ocupação no painel.
10. Equipe identifica falhas de pagamento, localização e notificação sem expor dados sensíveis.

## Fora do escopo

Várias cidades/transportadoras, apps nativos, ETA avançado, otimização de rotas, previsão de demanda, chatbot, fidelidade/descontos/assinatura, avaliações públicas, hardware próprio, múltiplos meios de pagamento, mapa avançado de assentos, integrações além do necessário, painéis avançados.

## Decisões pendentes (seção 9)

Cidade/rota/transportadora; responsável por atualizar dados; fonte de localização; assento numerado ou só quantidade; método/provedor de pagamento, retenção da vaga, cancelamento e reembolso; piloto com transações reais ou simuladas; canal de notificação; política de privacidade e retenção de localização; metas e duração do piloto.

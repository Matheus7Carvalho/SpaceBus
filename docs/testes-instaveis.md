# Testes instáveis

Registro de testes intermitentes encontrados: causa e correção (regras em `CLAUDE.md`, seção "Testes").

| Data | Teste | Causa | Correção |
|---|---|---|---|
| 17/11/2026 | `veiculos.spec` › edição com placa de outro veículo (mobile) | O `afterEach` de um teste paralelo apagava **todos** os veículos `E2E…`, inclusive o que este teste tinha acabado de criar ("Veículo não encontrado"). | Marca única por teste (`marca` no `beforeEach`); o `afterEach` apaga só os registros com a marca do próprio teste. Mesmo ajuste em `rotas.spec`. |
| 17/11/2026 | `rotas.spec` › cria com pontos (desktop) | Depois de clicar em "Nova rota", o `fill` do campo "Nome" rodava antes da troca de tela e preenchia o filtro "Nome" da listagem. O formulário ficava sem nome e o Salvar não enviava. | Esperar o `h1` da tela nova antes de preencher (também em veículos e viagens). |
| 24/11/2026 | `busca.spec` › encontra as viagens da seed (desktop e mobile) | Falha de dados, não de código. A viagem da seed das 12:30 tinha uma reserva **cancelada** de 17/11, de antes de o `reservas.spec` usar viagens próprias. O changeset que renova a seed não movia viagem com *qualquer* reserva, e ela ficou presa no passado, fora da busca de amanhã. | A renovação só deixa de mover a viagem se houver reserva que segura lugar (aguardando pagamento ou confirmada). O teardown do E2E agora limpa os dados de teste (`DELETE /api/dev/dados-teste`). |
| 24/11/2026 | `localizacao.spec` › Passageiro › mapa (desktop, só na primeira rodada) | O mapa (Leaflet) é carregado por import dinâmico. Na primeira abertura com o Vite de dev, ele descobria a dependência nova, pré-empacotava e recarregava a página ("optimized dependencies changed"); o teste via a tela recarregando no meio da verificação. Na segunda rodada passava. | `optimizeDeps.include: ['leaflet', 'react-leaflet']` no `vite.config.ts`: o Vite empacota o Leaflet ao subir, e a primeira abertura do mapa não recarrega mais a página. |

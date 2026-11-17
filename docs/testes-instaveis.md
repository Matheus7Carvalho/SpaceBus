# Testes instáveis

Registro de testes intermitentes encontrados: causa e correção (regras em `CLAUDE.md`, seção "Testes").

| Data | Teste | Causa | Correção |
|---|---|---|---|
| 17/11/2026 | `veiculos.spec` › edição com placa de outro veículo (mobile) | O `afterEach` de um teste paralelo apagava **todos** os veículos `E2E…`, inclusive o que este teste tinha acabado de criar ("Veículo não encontrado"). | Marca única por teste (`marca` no `beforeEach`); o `afterEach` apaga só os registros com a marca do próprio teste. Mesmo ajuste em `rotas.spec`. |
| 17/11/2026 | `rotas.spec` › cria com pontos (desktop) | Depois de clicar em "Nova rota", o `fill` do campo "Nome" rodava antes da troca de tela e preenchia o filtro "Nome" da listagem. O formulário ficava sem nome e o Salvar não enviava. | Esperar o `h1` da tela nova antes de preencher (também em veículos e viagens). |

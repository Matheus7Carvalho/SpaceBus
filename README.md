# SpaceBus

MVP para passageiros encontrarem, acompanharem, reservarem e pagarem viagens de van, e para a transportadora manter horários, capacidade e localização atualizados. Escopo em [docs/escopo-mvp.md](docs/escopo-mvp.md). Padrões de projeto em [CLAUDE.md](CLAUDE.md).

| Pasta | Conteúdo |
|---|---|
| `backend/` | API Spring Boot 3.5 (Java 21), PostgreSQL, Liquibase |
| `frontend/` | React + TypeScript (Vite), Tailwind, shadcn/ui |
| `docs/` | Escopo do MVP e documentação por módulo |
| `.claude/agents/` | Agentes do time: `product-owner`, `developer`, `qa-tester` |

## Módulos

| # | Módulo | Situação |
|---|---|---|
| 1 | [Cadastro operacional](docs/modulo-01-cadastro-operacional.md): veículos, rotas com pontos de parada, viagens (publicar, suspender, cancelar) | Em teste |
| 2 | [Consulta de viagens](docs/modulo-02-consulta-viagens.md): busca por origem, destino e data; detalhe com o percurso | Em teste |
| 3–6 | Reserva e pagamento, localização, notificações, indicadores | A fazer ([docs/escopo-mvp.md](docs/escopo-mvp.md)) |

## Requisitos

- Docker Desktop em execução
- Java 21 (`JAVA_HOME` apontando para a pasta do JDK, não para `bin`)
- Node.js 22+

## Rodar em modo desenvolvimento

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

- Frontend: http://localhost:5173
- API e Swagger: http://localhost:8080

O backend sobe o PostgreSQL do `compose.yaml` automaticamente e aplica as migrações com dados de demonstração. O `.env` é opcional; veja `.env.example`.

**Portas ocupadas?** Postgres, Vite e o proxy da API são configuráveis por variável de ambiente:

```bash
cd backend && POSTGRES_PORT=5433 ./mvnw spring-boot:run
```

```bash
cd frontend && FRONTEND_PORT=5174 npm run dev
```

## Rodar tudo em containers

```bash
docker compose --profile app up --build
```

Frontend em http://localhost:5173, API em http://localhost:8080.

## Contas de demonstração

Criadas pela seed do Liquibase (contexto `dev`). A tela de login tem atalhos para cada perfil.

| Perfil | E-mail | Senha | Acessa hoje |
|---|---|---|---|
| Administrador | `admin@spacebus.local` | `demo123` | Viagens, Rotas, Veículos, Buscar viagens |
| Operador | `operador@spacebus.local` | `demo123` | Viagens, Rotas, Veículos, Buscar viagens |
| Motorista | `motorista@spacebus.local` | `demo123` | Buscar viagens |
| Passageiro | `passageiro@spacebus.local` | `demo123` | Buscar viagens |

Há também um segundo motorista, `caio.nunes@spacebus.local`. A seed traz a rota "Centro – Aeroporto" (4 pontos), três veículos (um inativo) e quatro viagens (publicadas, rascunho e cancelada), com partidas a partir do dia seguinte à carga. As datas são fixadas na primeira carga; para renovar a seed: `docker compose down -v` e suba o backend de novo.

## Testes

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npx tsc -b && npm run lint && npm run build
```

Testes E2E (Playwright). Exigem o backend no ar com a seed de dev; o Vite é iniciado automaticamente se não estiver rodando. Na primeira vez, instale o navegador:

```bash
cd frontend && npx playwright install chromium
```

```bash
cd frontend && npm run test:e2e
```

Relatório em `frontend/playwright-report`. Para outra URL: `E2E_BASE_URL=http://host:porta npm run test:e2e`.

Antes dos testes, uma verificação aguarda até 60 s pelo frontend, pelo backend e pelo login da seed. Se o ambiente não estiver pronto, a suíte não roda e a mensagem indica a causa.


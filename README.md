# EduFin AI — Educação com Inteligência Artificial

Plataforma de educação financeira para adolescentes.

Aplicação web completa para ensinar finanças pessoais a adolescentes: metas financeiras, registro de
receitas e despesas, calculadora de juros, cursos progressivos, quizzes com correção no servidor,
gamificação saudável, notificações internas, relatórios, **tutor educativo integrado às aulas e aos quizzes**, comunidade moderada,
carteiras de investimento **simuladas** e canal de feedback — com área administrativa por permissões.

> Conteúdo educativo. A plataforma **não** movimenta dinheiro real, não se conecta a bancos e não faz
> recomendação de investimentos.

## Sumário

- [Tecnologias e versões](#tecnologias-e-versões)
- [Arquitetura em resumo](#arquitetura-em-resumo)
- [Executando localmente](#executando-localmente)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Banco de dados e migrações](#banco-de-dados-e-migrações)
- [Testes](#testes)
- [Documentação da API](#documentação-da-api)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Dados iniciais](#dados-iniciais)
- [Limitações conhecidas](#limitações-conhecidas)
- [Documentos complementares](#documentos-complementares)

## Tecnologias e versões

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem backend | Java (LTS) | **JDK 21** |
| Framework | Spring Boot (Web MVC, Data JPA, Security, Validation, Actuator, Mail) | **4.1.2** |
| ORM | Hibernate | 7 (gerenciado pelo Boot) |
| Banco (produção) | PostgreSQL | **17** |
| Banco (testes / perfil `local`) | H2 em modo PostgreSQL | 2.3.232 (versão verificada pelo Flyway) |
| Migrações | Flyway | gerenciado pelo Boot |
| Documentação da API | springdoc-openapi | 3.2.0 |
| Frontend | React + TypeScript + Vite | React 19, TS 7, Vite 8 |
| Dados no cliente | TanStack Query | 5 |
| Rotas | React Router | 7 |
| Gráficos | Recharts | 3 |
| Testes backend | JUnit 5, Spring Test/MockMvc, Testcontainers | gerenciados pelo Boot |
| Testes frontend | Vitest, Testing Library, MSW | Vitest 4 |
| Testes E2E | Playwright | 1.56 |
| Tipografia | Inter Variable (auto-hospedada) | @fontsource-variable/inter |

Justificativas detalhadas em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Arquitetura em resumo

**Monólito modular**: um único backend Spring Boot organizado por módulos de domínio (`user`, `goal`,
`transaction`, `calculator`, `quiz`, `course`, `gamification`, `notification`, `report`, `dashboard`,
`chatbot`, `forum`, `portfolio`, `feedback`, `admin`, `privacy`), cada um com entidade, repositório,
serviço, DTOs e controlador. O SPA React é compilado e servido pelo próprio Spring Boot (mesma origem),
o que permite autenticação por **sessão com cookie HttpOnly** + **proteção CSRF**, sem tokens expostos
ao JavaScript e sem CORS. Um único processo + PostgreSQL: simples de operar, fácil de evoluir.

```
Navegador ──► Spring Boot (porta 8080)
               ├── /            SPA React (arquivos estáticos)
               ├── /api/**      API REST (JSON, sessão + CSRF)
               └── /actuator/health
                     │
                     ▼
                PostgreSQL (Flyway)
```

## Executando localmente

### Pré-requisitos reais

- **JDK 21** e **Maven 3.9+** (backend)
- **Node.js 22 LTS** (ou ≥ 20.19) e npm (frontend)
- **Docker** (opcional: PostgreSQL via Compose e teste de migração com Testcontainers)

> Com Node 20.18 ou anterior, o npm pula silenciosamente as *bindings* nativas do Vite/Rolldown e do
> Oxlint (elas exigem Node ≥ 20.19). Se isso ocorrer, atualize o Node ou instale-as manualmente:
> `npm i --no-save @rolldown/binding-win32-x64-msvc @oxlint/binding-win32-x64-msvc` (ajuste o sufixo
> ao seu sistema).

### Opção A — Docker Compose (PostgreSQL + aplicação)

```bash
cp .env.example .env      # ajuste DB_PASSWORD, ADMIN_EMAIL e ADMIN_PASSWORD
docker compose up -d --build
```

Acesse http://localhost:8080. O administrador inicial é criado a partir de `ADMIN_EMAIL`/`ADMIN_PASSWORD`.

### Opção B — Sem Docker (H2 em arquivo, dados persistem em `backend/data/`)

```bash
cd frontend
npm ci
npm run build:backend          # compila o SPA e copia para backend/src/main/resources/static
cd ../backend
mvn -DskipTests package
ADMIN_EMAIL=admin@exemplo.com ADMIN_PASSWORD=TroqueJa123 \
  java -jar target/edufin-backend-1.0.0.jar --spring.profiles.active=local
```

No PowerShell, defina as variáveis com `$env:ADMIN_EMAIL='...'` antes do `java -jar`.

### Opção C — Desenvolvimento com recarga automática

```bash
# terminal 1 (backend, perfil local)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
# terminal 2 (frontend em http://localhost:5173, com proxy de /api para :8080)
cd frontend && npm run dev
```

## Variáveis de ambiente

Todas têm exemplo em [.env.example](.env.example). Nenhum segredo está no código.

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/edufin` | URL JDBC do PostgreSQL |
| `DB_USERNAME` / `DB_PASSWORD` | `edufin` / vazio | Credenciais do banco |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | vazio | Cria o primeiro administrador se ainda não existir |
| `APP_PUBLIC_URL` | `http://localhost:8080` | Base dos links enviados por e-mail |
| `COOKIE_SECURE` | `false` | Use `true` em produção (HTTPS) |
| `SESSION_TIMEOUT` | `2h` | Expiração da sessão por inatividade |
| `CORS_ALLOWED_ORIGINS` | vazio | Origens extras (separadas por vírgula) |
| `API_DOCS_ENABLED` | `true` | Habilita `/api/docs` e `/api/docs/ui` |
| `RATE_LIMIT_REGISTRATIONS_PER_HOUR` | `30` | Cadastros por IP por hora |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | desativado | SMTP para recuperação de senha |
| `RETENTION_ACTIVITY_DAYS` / `RETENTION_NOTIFICATION_DAYS` | `365` / `180` | Política de retenção |
| `REMINDERS_CRON` / `RETENTION_CRON` | 08:00 / 03:30 | Agendamento dos jobs (fuso `America/Sao_Paulo`) |

## Banco de dados e migrações

- **PostgreSQL** em produção; esquema versionado pelo **Flyway** (`backend/src/main/resources/db/migration`
  e `backend/src/main/java/db/migration`).
- O Hibernate roda com `ddl-auto=validate`: **nunca** altera o esquema; apenas confere que as entidades
  batem com as migrações (falha na subida se divergirem).
- `V1__schema.sql`: tabelas, chaves estrangeiras, `CHECK`s, unicidades e índices (SQL portável).
- `V2__SeedContent.java`: conteúdo educativo inicial (6 cursos, 21 aulas, 7 quizzes, 33 perguntas e 14 conquistas).
- Para alterar o esquema, crie `V3__descricao.sql` — migrações já aplicadas não devem ser editadas.
- Backup: ver [docs/OPERACAO.md](docs/OPERACAO.md).

## Testes

```bash
cd backend && mvn test                 # unitários + integração (H2) [+ PostgreSQL via Testcontainers se houver Docker]
cd frontend && npm test                # Vitest + Testing Library + MSW
cd frontend && npm run typecheck && npm run lint
cd frontend && npm run e2e             # Playwright: exige a aplicação rodando em http://localhost:8080
```

Os testes E2E usam o Chrome instalado (`channel: chrome`). Para usar o Chromium do Playwright:
`npx playwright install chromium` e `E2E_CHANNEL=chromium npm run e2e`. Outra URL: `E2E_BASE_URL=...`.

Resultados da última execução estão em [docs/RELATORIO-FINAL.md](docs/RELATORIO-FINAL.md).

## Documentação da API

- OpenAPI (JSON): `GET /api/docs` — interface interativa: `/api/docs/ui`
- Guia com fluxos e exemplos: [docs/API.md](docs/API.md)

## Estrutura do projeto

```
financial-education/
├── backend/                     Spring Boot (Maven)
│   └── src/main/java/br/finedu/
│       ├── common/              erros, paginação, dinheiro, rate limit, política de conteúdo, SPA
│       ├── security/            papéis, permissões, sessão, filtros, configuração
│       ├── user/                cadastro, login, perfil, preferências, histórico, recuperação de senha
│       ├── goal/  transaction/  calculator/  quiz/  course/  gamification/  notification/
│       ├── report/  dashboard/  chatbot/  forum/  portfolio/  feedback/  admin/  privacy/
│       └── (db/migration)       migração Java com o conteúdo inicial
├── frontend/                    React + Vite + TypeScript
│   ├── src/api                  cliente HTTP (CSRF, erros) e tipos da API
│   ├── src/components           design system (UI, formulários, modal, toasts, gráficos)
│   ├── src/pages                páginas
│   └── e2e/                     testes Playwright
├── docs/                        arquitetura, API, segurança/LGPD, operação, relatório final
├── Dockerfile  docker-compose.yml  .env.example
```

## Dados iniciais

As migrações criam apenas **conteúdo educativo** (cursos, aulas, quizzes, conquistas). Não há usuários
de demonstração nem dados financeiros fictícios apresentados como reais. O administrador inicial vem de
variáveis de ambiente.

## Limitações conhecidas

- **Recuperação de senha por e-mail** só funciona com SMTP configurado (`MAIL_ENABLED=true`); sem isso
  o sistema informa a indisponibilidade e um administrador pode gerar um **link de uso único** (30 min).
  O envio SMTP real não foi validado neste ambiente (sem servidor de e-mail).
- **Notificações** são internas à plataforma (sem e-mail/push).
- **Tutor IA**: usa uma base de explicações curadas (resposta principal, explicação alternativa e exemplo por tema), considera o tema da aula em estudo e dá dicas conceituais nos quizzes sem revelar a resposta. **Não há modelo de IA generativa integrado**; a interface informa isso ao aluno. A interface
  `ChatbotProvider` permite plugar um provedor externo.
- **Carteiras simuladas** não usam cotações reais (por decisão; nenhum provedor configurado).
- **Sessões e rate limit em memória**: adequados para uma instância. Para escalar horizontalmente,
  usar Spring Session JDBC e um limitador compartilhado.
- O teste de migração em **PostgreSQL real** (Testcontainers) é ignorado quando não há Docker; neste
  ambiente o Docker não estava disponível (ver relatório final).

## Documentos complementares

- [docs/ARQUITETURA.md](docs/ARQUITETURA.md) — decisões (ADRs) e organização
- [docs/DESIGN.md](docs/DESIGN.md) — identidade visual, tokens e componentes
- [docs/API.md](docs/API.md) — endpoints, regras e exemplos
- [docs/SEGURANCA-E-PRIVACIDADE.md](docs/SEGURANCA-E-PRIVACIDADE.md) — segurança, LGPD e proteção de menores
- [docs/OPERACAO.md](docs/OPERACAO.md) — implantação, backup, retenção e monitoramento
- [docs/RELATORIO-FINAL.md](docs/RELATORIO-FINAL.md) — resultados comprovados e pendências

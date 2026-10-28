# Arquitetura e decisões técnicas — EduFin AI

## Visão geral

Monólito modular: um backend Spring Boot que expõe a API REST e serve o SPA React compilado, com
PostgreSQL como única dependência de infraestrutura.

```
┌──────────── Navegador ────────────┐
│ SPA React (TanStack Query, Router)│
└──────────────┬────────────────────┘
               │ mesma origem: cookie de sessão HttpOnly + cabeçalho X-XSRF-TOKEN
┌──────────────▼──────────────────────────────────────────────────────────┐
│ Spring Boot 4.1 (JDK 21)                                                 │
│  security ─ filtros: sessão, revalidação do usuário, CSRF, autorização   │
│  módulos: user · goal · transaction · calculator · quiz · course ·       │
│           gamification · notification · report · dashboard · chatbot ·   │
│           forum · portfolio · feedback · admin · privacy                 │
│  common: erros, dinheiro, paginação, rate limit, política de conteúdo    │
│  jobs: lembretes diários, retenção de dados                              │
└──────────────┬──────────────────────────────────────────────────────────┘
               │ JPA/Hibernate (ddl validate) · Flyway
         ┌─────▼──────┐
         │ PostgreSQL │
         └────────────┘
```

Cada módulo segue o mesmo padrão: `Entidade` → `Repository` (Spring Data) → `Service` (regras e
transações) → `Controller` (HTTP fino) com `DTOs` (records) na entrada e na saída. Entidades JPA nunca
são serializadas diretamente.

## Decisões (ADRs resumidos)

### ADR-01 — Monólito modular em vez de microsserviços
**Contexto:** uma equipe pequena, domínio coeso, sem requisitos de escala independente por módulo.
**Decisão:** um único deployable com pacotes por domínio e dependências explícitas entre serviços.
**Consequências:** operação simples (1 processo + 1 banco), transações ACID entre módulos
(ex.: concluir aula → pontos → conquista → notificação na mesma transação). Para extrair um módulo no
futuro, as fronteiras por pacote já existem.

### ADR-02 — JDK 21 LTS
JDK 21 é LTS com suporte estendido, é o JDK disponível no ambiente e é plenamente suportado pelo
Spring Boot 4 (baseline Java 17). Records, `switch` com padrões e text blocks são usados no código.
O JDK 25 (LTS mais recente) também é compatível; a migração exige apenas trocar `java.version`.

### ADR-03 — Spring Boot 4.1.2
Linha atual e mantida (a 3.5 encerrou o suporte OSS). Traz Spring Framework 7, Spring Security 7,
Hibernate 7, Jakarta EE 11 e Jackson 3. Starters modulares (`spring-boot-starter-webmvc`,
`spring-boot-starter-flyway`, `*-test`) mantêm as dependências enxutas.

### ADR-04 — PostgreSQL + Flyway; H2 apenas para testes e avaliação local
PostgreSQL: robusto, gratuito, `NUMERIC` exato para dinheiro, bom suporte a restrições. Flyway versiona
o esquema; o Hibernate só **valida** (`ddl-auto=validate`), nunca altera o banco. As migrações usam SQL
portável para também rodarem em H2 (modo PostgreSQL), permitindo testes rápidos e um perfil `local` sem
Docker com **H2 em arquivo** (dados persistem). Um teste com Testcontainers valida as migrações no
PostgreSQL real quando há Docker.

### ADR-05 — Sessão no servidor + CSRF, em vez de JWT
O SPA é servido na mesma origem. Sessão com cookie `HttpOnly`/`SameSite=Lax` evita expor tokens ao
JavaScript (mitiga roubo por XSS), permite logout e revogação imediatos e dispensa CORS. A proteção CSRF
usa o padrão *double submit* (cookie `XSRF-TOKEN` + cabeçalho `X-XSRF-TOKEN`). Um "carimbo de
segurança" no usuário invalida sessões abertas quando a senha, o papel ou o status mudam.
**Limitação:** sessões em memória (1 instância). Para escalar, adicionar Spring Session JDBC.

### ADR-06 — Papéis com permissões granulares
Papéis `USER`, `EDUCATOR`, `MODERATOR`, `ADMIN` mapeiam permissões (`CONTENT_MANAGE`, `FORUM_MODERATE`,
`FEEDBACK_MANAGE`, `USER_MANAGE`, `ADMIN_AREA`). As regras de URL ficam centralizadas no
`SecurityConfig`; a posse dos dados (isolamento horizontal) é verificada nos serviços com consultas
`findByIdAndUserId`, retornando 404 para recursos de terceiros (sem revelar existência).

### ADR-07 — Dinheiro em `BigDecimal`/`NUMERIC(14,2)` e saldos derivados
Valores monetários nunca usam ponto flutuante. Saldos de metas e carteiras **não são armazenados**: são
sempre a soma das movimentações (fonte única de verdade). Movimentações bloqueiam a linha da meta/carteira
(`SELECT … FOR UPDATE`) para impedir saldo negativo por concorrência.

### ADR-08 — Gamificação idempotente por restrição de banco
Cada concessão de pontos é um evento com chave única `(usuário, tipo, referência)`. Ações "por item" usam
o id (uma aula só pontua uma vez); hábitos usam a data (no máximo uma vez por dia). Nenhuma regra depende
de quanto dinheiro a pessoa tem, e não há ranking público.

### ADR-09 — Quiz com correção exclusivamente no servidor
O cliente recebe perguntas sem a alternativa correta. Cada resposta é registrada uma única vez (restrição
única), e a nota é calculada a partir das respostas persistidas ao finalizar. O corpo enviado pelo cliente
no "finalizar" é ignorado.

### ADR-10 — Tutor com base de conhecimento local, contexto de estudo e interface plugável
Sem credenciais de IA disponíveis, o assistente usa respostas curadas (JSON versionado) e salvaguardas
independentes do provedor (bloqueio de senhas/cartões, recusa de recomendação personalizada). A interface
`ChatbotProvider` permite adicionar um LLM depois sem mudar controlador nem salvaguardas.
**Evolução (EduFin AI):** o tutor recebe o contexto da aula (`lessonId` → tema do curso), que prioriza respostas do mesmo tema; oferece os modos `ALTERNATIVE` e `EXAMPLE` sobre a resposta anterior (`entryId`), com textos curados; e expõe `POST /api/chatbot/hint/question/{id}`, que explica o conceito do tema da pergunta sem nunca devolver a alternativa correta. Frases-chave pontuam só pelas palavras significativas, para que perguntas específicas ("juros compostos") vençam as genéricas ("o que são juros").

### ADR-11 — Frontend React + Vite + TypeScript
SPA com React 19, TanStack Query (cache, estados de carregamento/erro, revalidação), React Router 7 e
Recharts. CSS próprio com *design tokens* (tema claro/escuro, contraste AA) em vez de framework de UI,
mantendo o bundle e as dependências pequenos. Rotas carregadas sob demanda (*code splitting*).

### ADR-12 — Integrações externas desativadas por padrão
E-mail (SMTP), notificações push, provedores de mercado e instituições financeiras não são simulados.
O que depende de credenciais fica atrás de configuração (`MAIL_ENABLED`) e a interface informa a
indisponibilidade com honestidade.

## Fluxos transacionais relevantes

| Operação | Garantias |
|---|---|
| Contribuição/retirada em meta | bloqueio da meta, saldo recalculado das movimentações, conclusão/reabertura automática, pontos e notificação na mesma transação |
| Concluir aula | progresso único por (usuário, aula); conclusão do curso quando todas as aulas estão feitas; pontos e conquistas idempotentes |
| Responder/finalizar quiz | bloqueio da tentativa, resposta única por pergunta, nota calculada no servidor |
| Rendimento simulado | um por mês por carteira (restrição única), só para meses encerrados |
| Denúncias | uma por pessoa por conteúdo; 3 abertas ocultam o conteúdo até revisão |

## Agendamentos

- `ReminderJob` (diário, 08:00 America/Sao_Paulo): prazos de metas (≤ 7 dias e vencidas) e lembrete de
  estudo após 3 dias sem aulas. Deduplicado por chave.
- `RetentionJob` (diário, 03:30): remove histórico de atividades antigo, notificações lidas antigas e tokens
  expirados.

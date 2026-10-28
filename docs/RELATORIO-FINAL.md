# Relatório final — EduFin

Data: 28/10/2026. Ambiente: Windows 11, JDK 21.0.4, Maven 3.9.16, Node 20.18.0, Chrome instalado.
Docker instalado, mas o daemon não estava em execução (não foi possível usá-lo).

## Escolhas técnicas
- **Arquitetura:** monólito modular (Spring Boot servindo API + SPA na mesma origem) — ver `docs/ARQUITETURA.md`.
- **JDK 21 LTS**, **Spring Boot 4.1.2** (Spring Security 7, Hibernate 7), **springdoc 3.2.0**.
- **Banco:** PostgreSQL 17 (produção) com Flyway; H2 em modo PostgreSQL para testes e para o perfil `local` (arquivo em disco).
- **Frontend:** React 19, TypeScript 7, Vite 8, React Router 7, TanStack Query 5, Recharts 3, lucide-react.
- **Testes:** JUnit 5 + MockMvc + Testcontainers; Vitest 4 + Testing Library + MSW; Playwright 1.56.

## Testes executados (resultados reais desta sessão)

| Suíte | Comando | Resultado |
|---|---|---|
| Backend (unitários + integração em H2) | `mvn test` | **112 testes, 0 falhas, 0 erros, 1 ignorado** |
| Migrações em PostgreSQL real (Testcontainers) | incluído em `mvn test` | **ignorado** — sem Docker em execução |
| Frontend (unitários, componentes, fluxos com MSW) | `npm test` | **29 de 29 aprovados** |
| Tipagem / lint | `npm run typecheck`, `npm run lint` | sem erros, 0 avisos |
| Build de produção | `npm run build`, `mvn package` | ok (bundle principal 282 kB, gráficos em chunk separado) |
| Ponta a ponta (Chrome, app real em H2 arquivo) | `npm run e2e` | **14 de 14 aprovados** |

Cobertura dos E2E (interface → API → banco → interface): cadastro, logout e login; criação de meta;
contribuições com conclusão automática; registro de receitas e despesas; dashboard com valores
persistidos; quiz completo com nota do servidor; inscrição, anotação e conclusão de aula; relatórios;
calculadora (compostos 121,00 / simples 120,00); publicação e comentário na comunidade, incluindo
bloqueio de telefone; notificações (leitura em lote); assistente e carteira simulada; redirecionamento
sem sessão; layout em celular sem rolagem horizontal em 9 páginas.

Testes de segurança incluídos no backend: acesso sem autenticação (10 rotas), CSRF ausente, acesso
horizontal a metas/transações/tentativas/notificações/carteiras/simulações/anotações de outro usuário,
tentativa de definir papel no cadastro, papel/permissões por perfil, manipulação de nota de quiz
(resposta dupla, alternativa de outra pergunta, nota enviada pelo cliente ignorada), farming de pontos,
força bruta no login (429), JSON malformado, enums inválidos, tentativas de injeção SQL em buscas,
sessões invalidadas após troca de senha/papel/desativação, link de redefinição de uso único.

## Problemas encontrados e corrigidos durante a verificação
- `csrf.spa()` do Spring Security 7 não troca o repositório de tokens para cookie → configurado `CookieCsrfTokenRepository` explicitamente.
- Bug no frontend detectado por teste: após o login, `queryClient.clear()` desconectava a consulta do usuário e a tela não saía do login → corrigido (troca de usuário sem remover a consulta ativa).
- `/actuator/health` retornava 503 por checar SMTP mesmo desativado → health de e-mail condicionado a `MAIL_ENABLED`.
- Limite de cadastros por IP (5/h) bloquearia turmas que compartilham IP → configurável, padrão 30/h.
- Alerta da calculadora quebrava o texto em colunas → corrigido.

## Critérios de aceitação

| # | Critério | Situação |
|---|---|---|
| 1 | Backend implementado e executável | **Atendido** (jar executado localmente) |
| 2 | Frontend integrado ao backend | **Atendido** (servido pelo Spring; E2E) |
| 3 | Persistência funcionando | **Atendido em H2 arquivo**; PostgreSQL **não verificado** nesta máquina (sem Docker) — SQL portável e teste Testcontainers prontos |
| 4 | Cadastro e autenticação | **Atendido** |
| 5 | Permissões verificadas no servidor | **Atendido** |
| 6 | Isolamento entre usuários | **Atendido** |
| 7 | Metas financeiras | **Atendido** |
| 8 | Histórico de transações persistido | **Atendido** |
| 9 | Calculadora matematicamente correta | **Atendido** (validada contra fórmula fechada) |
| 10 | Quizzes com nota do backend | **Atendido** |
| 11 | Progresso em cursos | **Atendido** |
| 12 | Gamificação conforme regras | **Atendido** |
| 13 | Relatórios a partir dos dados persistidos | **Atendido** |
| 14 | Chatbot dentro das capacidades implementadas | **Atendido** (base local; sem LLM) |
| 15 | Fórum com permissões e validações | **Atendido** |
| 16 | Carteiras identificadas como simuladas | **Atendido** |
| 17 | Mobile e desktop | **Atendido** (E2E mobile + revisão visual) |
| 18 | Fluxos principais com testes | **Atendido** |
| 19 | Documentação para outra pessoa executar | **Atendido** (README + docs) |
| 20 | Sem falhas críticas ocultadas | Ver pendências abaixo |

## Integrações que dependem de configuração
- **SMTP** para recuperação de senha (`MAIL_*`): implementado, **não validado** (sem servidor de e-mail). Alternativa funcional: link de uso único gerado por administrador.
- **Notificações externas (e-mail/push)**: não implementadas; apenas notificações internas.
- **Provedor de IA para o assistente**: não integrado; interface `ChatbotProvider` disponível.
- **Dados de mercado / instituições financeiras**: fora do escopo e desativados por decisão.

## Pendências e limitações
- Executar `mvn test` e `docker compose up` com Docker ativo para comprovar as migrações e a aplicação no PostgreSQL.
- Sessões e rate limit em memória (uma instância).
- Textos jurídicos (termos de uso e política de privacidade) precisam ser redigidos/revisados; não há fluxo de consentimento parental verificável (plataforma destinada a 12+).
- Interface administrativa de conteúdo cobre cursos, módulos, aulas e quizzes, mas não reordena itens existentes (novos itens entram ao final).
- Node 20.18 exige instalação manual das bindings nativas do Vite/Oxlint (recomendado Node 22 LTS).
- O projeto não é um repositório Git (a pasta original não era); nenhum commit foi feito.

---

# Modernização EduFin AI (28/10/2026)

## Estado encontrado
Typecheck e lint limpos, 29 testes de frontend e 112 de backend passando. Problemas: identidade genérica
(tema verde-água claro), navegação com finanças antes do aprendizado, painel com 12 blocos de mesmo peso,
aula sem navegação entre módulos nem progresso do curso, tutor apenas como chat livre (sem contexto, sem
modos, sem ajuda em exercícios), catálogo sem busca e com bug no selo "Etapa N" (usava o índice da lista
filtrada), marcação duplicada (voltar, estrelas). Na instância local em execução, o H2 2.4.240 passou a
recusar cadastros com `Check constraint invalid` (23514) após horas de uso.

## O que mudou
- **Identidade e design system:** tokens centralizados (escuro padrão, claro opcional), Inter auto-hospedada,
  roxo para IA, ciano para interação, gradiente só em pontos de IA. Ver `docs/DESIGN.md`.
- **Tutor (backend):** contexto da aula, modos "outra explicação" e "exemplo" (21 temas com textos curados),
  dica de quiz sem resposta, sugestões por tema, pontuação de frases corrigida; `generativeModel: false`
  exposto e mostrado ao aluno.
- **Telas:** página inicial (proposta, trilha real via `/api/public/courses`, exemplo de conversa rotulado),
  acesso (layout com painel da marca), Início do aluno reorganizado, catálogo com busca, aula com navegação
  de módulos (coluna ou lista recolhível) e painel do tutor, quiz com dica, página do Tutor IA.
- **Código:** `TutorChat`, `AuthLayout`, `BackLink`, `Stars` substituem duplicações; variante `ai` em botões e selos.
- **Correções:** selo de etapa do catálogo; navegação de aulas ausente em telas ≤ 1280 px (detectado pelo E2E);
  H2 fixado em 2.3.232 (versão verificada pelo Flyway).

## Validação executada
| Suíte | Resultado |
|---|---|
| Backend `mvn package` (testes incluídos) | **119 testes, 0 falhas, 1 ignorado** (PostgreSQL/Docker) |
| Frontend `vitest run` | **34/34** |
| `tsc -b` / `oxlint` | sem erros / 0 avisos |
| Playwright contra a aplicação em execução | **14/14** (inclui tutor na aula, dica de quiz, 390 px e 820 px sem rolagem horizontal) |
| Revisão visual | página inicial, login, Início (novo e com curso em andamento), catálogo, aula (1440 e 1280 px), tutor, quiz com dica, celular |

## Lacunas que permanecem
- Não há modelo de IA generativa: o tutor responde só com a base curada (perguntas fora dela recebem
  "ainda não sei responder"). A interface deixa isso explícito.
- O tutor não guarda histórico de conversa entre visitas (decisão de privacidade).
- A causa raiz do erro do H2 2.4.240 não foi isolada; a mitigação é a fixação de versão (PostgreSQL não é afetado).
- Testes em PostgreSQL real seguem dependentes de Docker em execução.

---

# Público para todas as idades (28/10/2026)

A plataforma deixou de ser voltada só para adolescentes e passou a atender qualquer pessoa a partir de 12 anos.
- Textos de aulas, quizzes e do tutor ficaram neutros (renda = salário, mesada ou bicos; exemplos de trabalho e
  estudo). Orientações para menores de idade foram mantidas como condicionais ("se você for menor de idade…").
- Categoria `ALLOWANCE` exibida como "Salário ou mesada"; página inicial, comunidade, README e documento de
  segurança atualizados. A idade mínima de 12 anos e a ciência do responsável para menores seguem no cadastro (LGPD).
- Migração `V3__AudienceNeutralContent` aplica os textos novos em bancos já existentes (aplicada com sucesso no H2 local).
- Validação: backend 119 testes (0 falhas, 1 ignorado — PostgreSQL/Docker), frontend 34/34, tsc e oxlint limpos,
  Playwright 14/14 contra a aplicação reconstruída.

# API — EduFin

A especificação OpenAPI completa é gerada pela aplicação em `GET /api/docs` (interface em `/api/docs/ui`).
Este guia resume os grupos de endpoints, as regras e o formato de erros.

## Convenções

- JSON em UTF-8; valores monetários com até 2 casas decimais; datas `yyyy-MM-dd`; instantes ISO-8601 (UTC).
- Autenticação por sessão (cookie `EDUFIN_SESSION`). Requisições `POST/PUT/DELETE` exigem o cabeçalho
  `X-XSRF-TOKEN` com o valor do cookie `XSRF-TOKEN` (obtido em `GET /api/auth/csrf`).
- Paginação: `?page=0&size=20` → `{ content, page, size, totalElements, totalPages }`.
- Erros sempre no formato:

```json
{ "timestamp": "...", "status": 422, "code": "INSUFFICIENT_BALANCE",
  "message": "Não é possível retirar mais do que o valor acumulado na meta (R$ 100.50).",
  "path": "/api/goals/5/movements", "fieldErrors": [] }
```

| Status | Uso |
|---|---|
| 400 `VALIDATION_ERROR` / `BAD_REQUEST` | campos inválidos (com `fieldErrors`) ou JSON malformado |
| 401 `UNAUTHENTICATED` / `BAD_CREDENTIALS` | sem sessão / login inválido (mensagem genérica) |
| 403 `FORBIDDEN` / `CSRF_INVALID` | sem permissão / token CSRF ausente |
| 404 `NOT_FOUND` | inexistente **ou de outro usuário** |
| 409 `CONFLICT`, `ALREADY_ANSWERED`, ... | duplicidade ou concorrência |
| 422 | regra de negócio violada (código específico) |
| 429 `RATE_LIMITED` | excesso de tentativas |
| 503 `RESET_UNAVAILABLE` | recuperação de senha sem SMTP configurado |

## Endpoints

### Autenticação e conta
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/auth/csrf` | emite o cookie `XSRF-TOKEN` |
| POST | `/api/auth/register` | cadastro (`email`, `password`, `displayName`, `acceptTerms`, `ageConfirmation`) — já autentica |
| POST | `/api/auth/login` | login (`email`, `password`) |
| POST | `/api/auth/logout` | encerra a sessão |
| POST | `/api/auth/password-reset/request` · `/confirm` | recuperação de senha (requer SMTP) / troca com token |
| GET/PUT | `/api/me` | perfil / alterar apelido |
| PUT | `/api/me/email` · `/api/me/password` | alterar e-mail (com senha) / senha |
| GET/PUT | `/api/me/preferences` | tema, notificações, meta semanal de aulas |
| GET | `/api/me/activity` | histórico de atividades (paginado) |
| GET | `/api/me/export` | exportação dos dados pessoais (JSON) |
| DELETE | `/api/me` | exclusão definitiva da conta (`{ "password": "..." }`) |
| GET | `/api/public/config` | recursos disponíveis (ex.: recuperação por e-mail) |

### Finanças
| Método | Caminho | Descrição |
|---|---|---|
| GET/POST | `/api/goals` | listar (`?status=ACTIVE|COMPLETED|CANCELLED|ALL`) / criar |
| GET/PUT | `/api/goals/{id}` | detalhar / editar |
| POST | `/api/goals/{id}/cancel` | cancelar |
| GET/POST | `/api/goals/{id}/movements` | histórico / `CONTRIBUTION` ou `WITHDRAWAL` |
| GET/POST | `/api/transactions` | listar com filtros (`from`, `to`, `type`, `category`, `origin`, `q`, `sort`, `direction`) / criar |
| GET/PUT/DELETE | `/api/transactions/{id}` | detalhar / editar / excluir |
| GET | `/api/transactions/summary` | receitas, despesas, saldo e totais por categoria |
| GET | `/api/transactions/categories` | categorias e tipo |
| POST | `/api/calculator/calculate` · `/compare` | simular / comparar 2–4 cenários |
| GET/POST/DELETE | `/api/calculator/simulations[/{id}]` | simulações salvas (resultado recalculado no servidor) |
| GET/POST | `/api/wallets` | carteiras simuladas |
| GET/PUT/DELETE | `/api/wallets/{id}` | detalhe (desempenho, evolução, aviso) / editar / excluir |
| POST | `/api/wallets/{id}/archive?archived=true` | arquivar/reativar |
| GET/POST | `/api/wallets/{id}/movements` | aportes e retiradas simulados |
| POST | `/api/wallets/{id}/returns` | rendimento simulado de um mês encerrado (`{ "period": "2026-09" }`) |
| POST | `/api/wallets/{id}/scenarios` | projeção com taxas hipotéticas |
| GET | `/api/reports?from&to` | relatório completo do período |
| GET | `/api/dashboard` | painel inicial |

### Aprendizado
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/courses` (`?topic&difficulty`) · `/api/courses/mine` · `/api/courses/notes` | catálogo, meus cursos, anotações |
| GET | `/api/courses/{id}` | detalhe com módulos, progresso e próxima aula |
| POST | `/api/courses/{id}/enroll` | inscrição (idempotente) |
| GET/PUT | `/api/courses/{id}/reviews` · `/review` | avaliações públicas / minha avaliação (1–5) |
| GET | `/api/lessons/{id}` | aula (requer inscrição) |
| POST | `/api/lessons/{id}/visit` · `/complete` | "continuar de onde parou" / concluir |
| PUT | `/api/lessons/{id}/note` | anotação privada |
| GET | `/api/quizzes` · `/api/quizzes/{id}` | catálogo e histórico do quiz |
| POST | `/api/quizzes/{id}/attempts` | inicia ou retoma tentativa (sem gabarito) |
| POST | `/api/quiz-attempts/{id}/answers` | responde uma pergunta → correção e explicação |
| POST | `/api/quiz-attempts/{id}/finish` | nota calculada no servidor + recomendações |
| GET | `/api/quiz-attempts/{id}` · `/result` · `/history` | tentativa / resultado / histórico |
| GET | `/api/gamification/summary` · `/history` | pontos, nível, sequência, conquistas, desafios |
| POST | `/api/chatbot/ask` · GET `/api/chatbot/info` | assistente educativo |

### Comunidade, notificações e feedback
| Método | Caminho | Descrição |
|---|---|---|
| GET/POST | `/api/forum/posts` (`?q&category&mine`) | listar / publicar |
| GET/PUT/DELETE | `/api/forum/posts/{id}` | ver / editar ou excluir (autor) |
| GET/POST | `/api/forum/posts/{id}/comments` | comentários |
| PUT/DELETE | `/api/forum/comments/{id}` | editar/excluir comentário (autor) |
| POST/DELETE | `/api/forum/posts/{id}/like` | curtir / descurtir (idempotente) |
| POST | `/api/forum/posts/{id}/report` · `/api/forum/comments/{id}/report` | denunciar |
| GET | `/api/notifications` (`?unreadOnly`) · `/unread-count` | notificações internas |
| POST | `/api/notifications/{id}/read` · `/read-all` · DELETE `/{id}` | marcar/excluir |
| GET/POST | `/api/feedback` | meus feedbacks / enviar |

### Administração (por permissão)
| Permissão | Caminhos |
|---|---|
| `ADMIN_AREA` | `GET /api/admin/stats` |
| `USER_MANAGE` | `/api/admin/users` (busca), `PUT /{id}/role`, `PUT /{id}/status`, `POST /{id}/reset-link` |
| `CONTENT_MANAGE` | `/api/admin/content/courses`, `/modules/{id}`, `/lessons/{id}`, `/quizzes`, `/quizzes/{id}/questions` |
| `FORUM_MODERATE` | `/api/admin/moderation/reports`, `/reports/{id}/resolve`, `/posts`, `/comments`, `/reviews` |
| `FEEDBACK_MANAGE` | `GET /api/admin/feedback`, `PUT /api/admin/feedback/{id}` |

## Exemplo (curl)

```bash
B=http://localhost:8080
curl -c c.txt -s $B/api/auth/csrf > /dev/null
T=$(grep XSRF-TOKEN c.txt | awk '{print $7}')
curl -b c.txt -c c.txt -H "X-XSRF-TOKEN: $T" -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","password":"Senha123forte","displayName":"ana","acceptTerms":true,"ageConfirmation":true}' \
  $B/api/auth/register
curl -b c.txt -c c.txt -H "X-XSRF-TOKEN: $T" -H 'Content-Type: application/json' \
  -d '{"title":"Fone","category":"ELECTRONICS","targetAmount":300}' $B/api/goals
curl -b c.txt $B/api/dashboard
```

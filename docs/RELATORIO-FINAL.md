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

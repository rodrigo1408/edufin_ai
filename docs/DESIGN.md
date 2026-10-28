# Design system — EduFin AI

Identidade: **EduFin AI — Educação com Inteligência Artificial**. Tema padrão escuro (azul-marinho),
roxo para recursos de IA e ação principal, ciano para interação e indicadores.

## Tokens (`frontend/src/styles/global.css`)

Todas as cores, raios, sombras, espaçamentos e a tipografia ficam em variáveis CSS no `:root`
(tema escuro, padrão) e em `:root[data-theme='light']` (tema claro, escolhido em Perfil).

| Token | Escuro | Uso |
|---|---|---|
| `--bg` / `--surface` / `--surface-2` / `--surface-3` | `#0a1020` / `#111a2e` / `#172239` / `#1e2b47` | fundo, cards, áreas internas |
| `--text` / `--text-muted` / `--text-subtle` | `#e8edf7` / `#9eabc6` / `#7d8bab` | texto (contraste AA sobre `--surface`) |
| `--primary` | `#7c5cff` | ação principal e recursos de IA |
| `--cyan` | `#22d3ee` | links, foco, progresso, ações secundárias |
| `--ai-gradient` | roxo → ciano | uso pontual: marca, tutor, CTA de IA |
| `--success` / `--warning` / `--danger` | verde / âmbar / vermelho | estados |
| `--chart-1..6` | ciano, âmbar, lilás, rosa, verde, azul | gráficos |

Tipografia: **Inter Variable** auto-hospedada (`@fontsource-variable/inter`), compatível com a CSP
(`font-src 'self'`). Foco visível em ciano; `prefers-reduced-motion` desliga animações.

## Componentes compartilhados

| Componente | Arquivo | Uso |
|---|---|---|
| `Button`, `ButtonLink` (variantes `primary`, `secondary`, `ghost`, `danger`, `ai`) | `components/ui.tsx` | todas as ações |
| `Card`, `PageHeader`, `Stat`, `Badge` (tom `ai`), `ProgressBar`, `Tabs`, `Chips`, `Pagination` | `components/ui.tsx` | estrutura das páginas |
| `EmptyState`, `ErrorState`, `Spinner`, `Alert` | `components/ui.tsx` | estados vazio, erro, carregamento |
| `BackLink`, `Stars` | `components/ui.tsx` | substituem marcação repetida em 6 e 2 páginas |
| `TutorChat` | `components/TutorChat.tsx` | conversa com o tutor (página do tutor e painel da aula) |
| `AuthLayout` | `components/AuthLayout.tsx` | login, cadastro e redefinição de senha |
| `Logo` | `components/Logo.tsx` | marca |
| `Modal`, `ConfirmDialog`, `Toast`, campos de formulário, gráficos | `components/*` | inalterados na API, restilizados pelos tokens |

## Organização da experiência

- **Navegação**: "Aprender" (Início, Cursos e trilha, Tutor IA, Quizzes, Conquistas) vem antes de
  "Praticar com dinheiro" (Metas, Transações, Relatórios, Calculadora, Carteiras) e "Comunidade e conta".
- **Início**: continuar a aula atual + acesso ao tutor → progresso → próximas atividades e conquistas
  → finanças → notificações e atividades.
- **Aula**: lista de módulos/aulas com progresso (coluna em telas ≥ 1281 px; lista recolhível abaixo
  disso), conteúdo com largura de leitura confortável e painel do tutor com o contexto da aula.
- **Quiz**: "Pedir dica ao tutor" antes de responder (conceito, sem a alternativa correta).

## Responsividade verificada

Teste E2E `e2e/mobile.spec.ts`: sem rolagem horizontal em 390 px e 820 px nas páginas principais,
menu lateral recolhível, atalho para o tutor na barra superior do celular.

import { useQuery } from '@tanstack/react-query'
import { BookOpen, Calculator, Clock, Route, ShieldCheck, Sparkles, Target, Trophy } from 'lucide-react'
import { api } from '../api/client'
import type { PublicCourse } from '../api/types'
import { Logo } from '../components/Logo'
import { Badge, ButtonLink } from '../components/ui'
import { difficultyLabel } from '../utils/labels'

const features = [
  { icon: <Sparkles size={20} />, ai: true, title: 'Tutor que acompanha a aula', text: 'Pergunte durante a aula, peça outra explicação ou um exemplo. Nos quizzes, ele dá dicas sem entregar a resposta.' },
  { icon: <Route size={20} />, title: 'Trilha do básico ao avançado', text: 'Orçamento, poupança, juros, crédito e investimentos, em aulas curtas e na ordem certa.' },
  { icon: <Target size={20} />, title: 'Pratique com metas reais', text: 'Defina metas, registre o que entra e sai e acompanhe a evolução nos relatórios.' },
  { icon: <Calculator size={20} />, title: 'Simuladores', text: 'Calculadora de juros e carteiras 100% simuladas para testar ideias sem dinheiro real.' },
  { icon: <Trophy size={20} />, title: 'Progresso que motiva', text: 'Pontos por aprender e criar bons hábitos, sem ranking de quem tem mais dinheiro.' },
  { icon: <ShieldCheck size={20} />, title: 'Privacidade em primeiro lugar', text: 'Seus registros são só seus. Pedimos o mínimo de dados e nunca senhas de banco.' },
]

export function LandingPage() {
  const courses = useQuery({ queryKey: ['public-courses'], queryFn: () => api.get<PublicCourse[]>('/api/public/courses') })

  return (
    <div className="landing">
      <nav className="landing-nav" aria-label="Navegação inicial">
        <Logo to="/" />
        <div className="row">
          <ButtonLink to="/entrar" variant="ghost">Entrar</ButtonLink>
          <ButtonLink to="/cadastro">Criar conta grátis</ButtonLink>
        </div>
      </nav>
      <main>
        <section className="hero">
          <div>
            <span className="eyebrow">Educação com Inteligência Artificial</span>
            <h1>
              Aprenda a cuidar do seu dinheiro com um <span className="gradient-text">tutor ao seu lado</span>.
            </h1>
            <p className="lead">
              Cursos curtos e progressivos de educação financeira, com um tutor que explica de outro jeito quando você não entende — e simuladores para praticar sem risco.
            </p>
            <div className="row" style={{ marginTop: 24 }}>
              <ButtonLink to="/cadastro" variant="ai">Começar a aprender</ButtonLink>
              <a href="#trilha" className="btn btn-secondary">Ver a trilha de cursos</a>
            </div>
            <p className="small muted" style={{ marginTop: 16 }}>Gratuito · para estudantes a partir de 12 anos · sem anúncios</p>
          </div>

          <figure className="ai-surface" style={{ margin: 0, padding: 20 }} aria-label="Exemplo de conversa com o tutor">
            <div className="tutor-head" style={{ marginBottom: 14 }}>
              <span className="ai-mark" aria-hidden><Sparkles size={18} /></span>
              <div>
                <strong>Tutor EduFin AI</strong>
                <span className="small muted">Aula: Juros simples x compostos</span>
              </div>
            </div>
            <div className="chat" style={{ minHeight: 0, maxHeight: 'none', overflow: 'visible' }}>
              <div className="bubble user">Não entendi juros compostos 😕</div>
              <div className="bubble bot">
                São como uma bola de neve: os juros de cada mês passam a render também. R$ 1.000 a 1% ao mês viram cerca de R$ 1.126,83 em um ano.
              </div>
              <div className="chip-group">
                <span className="chip ai" aria-hidden>Explicar de outro jeito</span>
                <span className="chip ai" aria-hidden>Dar um exemplo</span>
              </div>
            </div>
            <figcaption className="small muted" style={{ marginTop: 12 }}>Exemplo ilustrativo de conversa. O tutor responde com explicações curadas pela equipe.</figcaption>
          </figure>
        </section>

        <section className="landing-section" aria-labelledby="como">
          <h2 id="como">Como você aprende aqui</h2>
          <p className="muted">Três passos, no seu ritmo.</p>
          <div className="grid grid-3 steps" style={{ marginTop: 16 }}>
            <div className="card"><h3>Estude uma aula curta</h3><p className="small muted">Conteúdo direto, com exemplos do dia a dia de quem está começando.</p></div>
            <div className="card"><h3>Tire dúvidas com o tutor</h3><p className="small muted">Ele sabe qual aula você está estudando e oferece explicações alternativas e exemplos.</p></div>
            <div className="card"><h3>Pratique e acompanhe</h3><p className="small muted">Faça quizzes, crie metas e veja sua evolução em pontos, conquistas e relatórios.</p></div>
          </div>
        </section>

        <section className="landing-section" id="trilha" aria-labelledby="trilha-titulo">
          <h2 id="trilha-titulo">A trilha de cursos</h2>
          <p className="muted">Do primeiro orçamento aos fundamentos de investimentos.</p>
          {courses.isLoading && <p className="muted">Carregando cursos…</p>}
          {courses.error && <p className="muted">Não foi possível carregar a trilha agora. Crie sua conta para ver o catálogo completo.</p>}
          <ol className="path" style={{ marginTop: 18 }}>
            {courses.data?.map((c, i) => (
              <li key={c.id} className="path-item">
                <span className="path-step" aria-hidden>{i + 1}</span>
                <div className="card" style={{ padding: 16 }}>
                  <div className="row-between">
                    <h3 style={{ margin: 0 }}>{c.title}</h3>
                    <div className="row" style={{ gap: 6 }}>
                      <Badge tone="primary">{c.categoryLabel}</Badge>
                      <Badge>{difficultyLabel[c.difficulty]}</Badge>
                    </div>
                  </div>
                  <p className="small muted" style={{ margin: '6px 0 0' }}>{c.summary}</p>
                  <div className="row small muted" style={{ marginTop: 8, gap: 14 }}>
                    <span className="row" style={{ gap: 4 }}><BookOpen size={14} aria-hidden /> {c.lessonCount} aulas</span>
                    <span className="row" style={{ gap: 4 }}><Clock size={14} aria-hidden /> {c.estimatedMinutes} min</span>
                  </div>
                </div>
              </li>
            ))}
          </ol>
        </section>

        <section className="landing-section" aria-labelledby="recursos">
          <h2 id="recursos">Tudo em um só lugar</h2>
          <div className="grid grid-3" style={{ marginTop: 16 }}>
            {features.map((f) => (
              <div key={f.title} className="card">
                <div className={`feature-icon ${f.ai ? 'ai' : ''}`} aria-hidden>{f.icon}</div>
                <h3>{f.title}</h3>
                <p className="small muted" style={{ margin: 0 }}>{f.text}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="landing-section">
          <div className="card ai-surface row-between" style={{ padding: 24 }}>
            <div>
              <h2 style={{ marginBottom: 4 }}>Pronto para começar?</h2>
              <p className="muted" style={{ margin: 0 }}>Crie sua conta e faça a primeira aula em menos de 10 minutos.</p>
            </div>
            <ButtonLink to="/cadastro" variant="ai">Criar conta grátis</ButtonLink>
          </div>
          <p className="small muted" style={{ marginTop: 24 }}>
            Conteúdo educativo. Não é consultoria financeira nem recomendação de investimento. A plataforma não movimenta dinheiro real; decisões financeiras devem ser tomadas com seus responsáveis.
          </p>
        </section>
        <p className="small muted" style={{ textAlign: 'center' }}>EduFin AI · Educação com Inteligência Artificial</p>
      </main>
    </div>
  )
}

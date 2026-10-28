import { BookOpen, Calculator, FlaskConical, ShieldCheck, Target, Trophy } from 'lucide-react'
import { Logo } from '../components/Logo'
import { ButtonLink } from '../components/ui'

const features = [
  { icon: <Target size={20} />, title: 'Metas com propósito', text: 'Defina o que quer conquistar, registre cada contribuição e acompanhe o progresso.' },
  { icon: <BookOpen size={20} />, title: 'Cursos curtos', text: 'Trilha do básico ao avançado: orçamento, poupança, juros, crédito e investimentos.' },
  { icon: <Calculator size={20} />, title: 'Calculadora de juros', text: 'Compare juros simples e compostos com gráficos e explicações passo a passo.' },
  { icon: <FlaskConical size={20} />, title: 'Laboratório de investimentos', text: 'Carteiras 100% simuladas para praticar sem dinheiro real.' },
  { icon: <Trophy size={20} />, title: 'Conquistas e desafios', text: 'Ganhe pontos aprendendo e criando bons hábitos — sem ranking de quem tem mais dinheiro.' },
  { icon: <ShieldCheck size={20} />, title: 'Privacidade em primeiro lugar', text: 'Seus registros são só seus. Pedimos o mínimo de dados e nunca senhas de banco.' },
]

export function LandingPage() {
  return (
    <div className="landing">
      <nav className="landing-nav" aria-label="Navegação inicial">
        <Logo to="/" />
        <div className="row">
          <ButtonLink to="/entrar" variant="ghost">Entrar</ButtonLink>
          <ButtonLink to="/cadastro">Criar conta</ButtonLink>
        </div>
      </nav>
      <main>
        <section className="hero">
          <div>
            <span className="badge badge-primary">Educação financeira para adolescentes</span>
            <h1 style={{ fontSize: 'clamp(2rem, 4vw, 2.8rem)', margin: '14px 0' }}>
              Aprenda a cuidar do seu dinheiro hoje — e realize seus planos amanhã.
            </h1>
            <p className="muted" style={{ fontSize: '1.1rem' }}>
              Cursos curtos, quizzes, metas e simuladores em um só lugar. Gratuito, sem anúncios e sem promessas de dinheiro fácil.
            </p>
            <div className="row" style={{ marginTop: 20 }}>
              <ButtonLink to="/cadastro">Começar agora</ButtonLink>
              <ButtonLink to="/entrar" variant="secondary">Já tenho conta</ButtonLink>
            </div>
          </div>
          <div className="card" aria-hidden>
            <div className="stack">
              <div className="row-between"><strong>Meta: fone novo</strong><span className="badge badge-success">72%</span></div>
              <div className="progress"><span style={{ width: '72%' }} /></div>
              <p className="small muted">Exemplo ilustrativo de como você acompanha uma meta.</p>
              <div className="row-between"><span>Aulas concluídas</span><strong>8 de 12</strong></div>
              <div className="progress accent"><span style={{ width: '66%' }} /></div>
            </div>
          </div>
        </section>
        <section aria-labelledby="recursos">
          <h2 id="recursos">O que você encontra aqui</h2>
          <div className="grid grid-3" style={{ marginTop: 12 }}>
            {features.map((f) => (
              <div key={f.title} className="card">
                <div className="feature-icon" aria-hidden>{f.icon}</div>
                <h3>{f.title}</h3>
                <p className="muted small">{f.text}</p>
              </div>
            ))}
          </div>
        </section>
        <p className="small muted" style={{ marginTop: 32 }}>
          Conteúdo educativo. Não é consultoria financeira nem recomendação de investimento. Decisões financeiras reais devem ser tomadas com seus responsáveis.
        </p>
      </main>
    </div>
  )
}

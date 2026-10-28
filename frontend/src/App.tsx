import { lazy, Suspense, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useAuth } from './auth/AuthContext'
import { Layout } from './components/Layout'
import { Spinner } from './components/ui'
import type { Permission } from './api/types'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const GoalsPage = lazy(() => import('./pages/GoalsPage').then((m) => ({ default: m.GoalsPage })))
const GoalDetailPage = lazy(() => import('./pages/GoalDetailPage').then((m) => ({ default: m.GoalDetailPage })))
const TransactionsPage = lazy(() => import('./pages/TransactionsPage').then((m) => ({ default: m.TransactionsPage })))
const CalculatorPage = lazy(() => import('./pages/CalculatorPage').then((m) => ({ default: m.CalculatorPage })))
const QuizzesPage = lazy(() => import('./pages/QuizzesPage').then((m) => ({ default: m.QuizzesPage })))
const QuizPlayPage = lazy(() => import('./pages/QuizPlayPage').then((m) => ({ default: m.QuizPlayPage })))
const QuizResultPage = lazy(() => import('./pages/QuizResultPage').then((m) => ({ default: m.QuizResultPage })))
const CoursesPage = lazy(() => import('./pages/CoursesPage').then((m) => ({ default: m.CoursesPage })))
const CourseDetailPage = lazy(() => import('./pages/CourseDetailPage').then((m) => ({ default: m.CourseDetailPage })))
const LessonPage = lazy(() => import('./pages/LessonPage').then((m) => ({ default: m.LessonPage })))
const GamificationPage = lazy(() => import('./pages/GamificationPage').then((m) => ({ default: m.GamificationPage })))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage })))
const ReportsPage = lazy(() => import('./pages/ReportsPage').then((m) => ({ default: m.ReportsPage })))
const ChatbotPage = lazy(() => import('./pages/ChatbotPage').then((m) => ({ default: m.ChatbotPage })))
const CommunityPage = lazy(() => import('./pages/CommunityPage').then((m) => ({ default: m.CommunityPage })))
const PostPage = lazy(() => import('./pages/PostPage').then((m) => ({ default: m.PostPage })))
const WalletsPage = lazy(() => import('./pages/WalletsPage').then((m) => ({ default: m.WalletsPage })))
const WalletDetailPage = lazy(() => import('./pages/WalletDetailPage').then((m) => ({ default: m.WalletDetailPage })))
const FeedbackPage = lazy(() => import('./pages/FeedbackPage').then((m) => ({ default: m.FeedbackPage })))
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })))
const AdminPage = lazy(() => import('./pages/AdminPage').then((m) => ({ default: m.AdminPage })))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })))

function RequireAuth({ children, permission }: { children: ReactNode; permission?: Permission }) {
  const { user, loading, can } = useAuth()
  const location = useLocation()
  if (loading) return <Spinner />
  if (!user) return <Navigate to="/entrar" replace state={{ from: location.pathname }} />
  if (permission && !can(permission)) return <Navigate to="/painel" replace />
  return <>{children}</>
}

function PublicOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  return user ? <Navigate to="/painel" replace /> : <>{children}</>
}

export default function App() {
  return (
    <Suspense fallback={<Spinner />}>
    <Routes>
      <Route path="/" element={<PublicOnly><LandingPage /></PublicOnly>} />
      <Route path="/entrar" element={<PublicOnly><LoginPage /></PublicOnly>} />
      <Route path="/cadastro" element={<PublicOnly><RegisterPage /></PublicOnly>} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/painel" element={<DashboardPage />} />
        <Route path="/metas" element={<GoalsPage />} />
        <Route path="/metas/:id" element={<GoalDetailPage />} />
        <Route path="/transacoes" element={<TransactionsPage />} />
        <Route path="/calculadora" element={<CalculatorPage />} />
        <Route path="/quizzes" element={<QuizzesPage />} />
        <Route path="/quizzes/:id" element={<QuizPlayPage />} />
        <Route path="/quiz-tentativas/:id" element={<QuizResultPage />} />
        <Route path="/cursos" element={<CoursesPage />} />
        <Route path="/cursos/:id" element={<CourseDetailPage />} />
        <Route path="/aulas/:id" element={<LessonPage />} />
        <Route path="/gamificacao" element={<GamificationPage />} />
        <Route path="/notificacoes" element={<NotificationsPage />} />
        <Route path="/relatorios" element={<ReportsPage />} />
        <Route path="/assistente" element={<ChatbotPage />} />
        <Route path="/comunidade" element={<CommunityPage />} />
        <Route path="/comunidade/:id" element={<PostPage />} />
        <Route path="/carteiras" element={<WalletsPage />} />
        <Route path="/carteiras/:id" element={<WalletDetailPage />} />
        <Route path="/feedback" element={<FeedbackPage />} />
        <Route path="/perfil" element={<ProfilePage />} />
        <Route path="/admin" element={<RequireAuth permission="ADMIN_AREA"><AdminPage /></RequireAuth>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
    </Suspense>
  )
}

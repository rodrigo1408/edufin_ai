// Contratos da API (espelham os DTOs do backend). Valores monetários chegam como number com 2 casas
// e são apenas exibidos/formatados no cliente; todos os cálculos oficiais acontecem no servidor.

export type Role = 'USER' | 'EDUCATOR' | 'MODERATOR' | 'ADMIN'
export type Permission = 'ADMIN_AREA' | 'CONTENT_MANAGE' | 'FORUM_MODERATE' | 'FEEDBACK_MANAGE' | 'USER_MANAGE'
export type Theme = 'SYSTEM' | 'LIGHT' | 'DARK'
export type Topic = 'BUDGET' | 'CONSCIOUS_CONSUMPTION' | 'SAVING' | 'INTEREST' | 'INFLATION' | 'CREDIT' | 'DEBT' | 'INVESTING' | 'GOALS'
export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Preferences {
  theme: Theme
  notifyGoals: boolean
  notifyLearning: boolean
  notifyAchievements: boolean
  notifyCommunity: boolean
  weeklyLessonTarget: number
}

export interface Me {
  id: number
  email: string
  displayName: string
  role: Role
  permissions: Permission[]
  createdAt: string
  lastLoginAt: string | null
  preferences: Preferences
}

export interface AchievementView {
  code: string
  title: string
  description: string
  icon: string
  points: number
  unlocked: boolean
  unlockedAt: string | null
}

export interface Reward {
  points: number
  achievements: AchievementView[]
  challenges: string[]
}

export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
export type GoalCategory = 'ELECTRONICS' | 'EDUCATION' | 'TRAVEL' | 'EMERGENCY_FUND' | 'LEISURE' | 'GIFT' | 'CLOTHING' | 'OTHER'

export interface Goal {
  id: number
  title: string
  description: string | null
  category: GoalCategory
  targetAmount: number
  savedAmount: number
  remainingAmount: number
  progressPercent: number
  deadline: string | null
  daysRemaining: number | null
  suggestedMonthlySaving: number | null
  status: GoalStatus
  createdAt: string
  completedAt: string | null
}

export interface GoalMovement {
  id: number
  type: 'CONTRIBUTION' | 'WITHDRAWAL'
  amount: number
  note: string | null
  createdAt: string
}

export type TxType = 'INCOME' | 'EXPENSE'
export type TxOrigin = 'MANUAL' | 'SIMULATED' | 'EXTERNAL'

export interface Transaction {
  id: number
  description: string
  amount: number
  type: TxType
  category: string
  categoryLabel: string
  occurredOn: string
  notes: string | null
  origin: TxOrigin
  goalId: number | null
  createdAt: string
}

export interface CategoryOption {
  value: string
  label: string
  type: TxType
}

export interface CategoryTotal {
  category: string
  label: string
  total: number
  count: number
}

export interface TxSummary {
  from: string
  to: string
  income: number
  expense: number
  balance: number
  expensesByCategory: CategoryTotal[]
  incomeByCategory: CategoryTotal[]
}

export type InterestType = 'SIMPLE' | 'COMPOUND'
export type RateUnit = 'MONTHLY' | 'YEARLY'
export type PeriodUnit = 'MONTHS' | 'YEARS'

export interface CalcInput {
  interestType: InterestType
  principal: number
  ratePercent: number
  rateUnit: RateUnit
  period: number
  periodUnit: PeriodUnit
  monthlyContribution: number
}

export interface CalcPoint {
  month: number
  invested: number
  interest: number
  balance: number
}

export interface CalcResult {
  finalAmount: number
  totalInvested: number
  totalInterest: number
  monthlyRatePercent: number
  months: number
  series: CalcPoint[]
}

export interface CalcResponse {
  input: CalcInput
  result: CalcResult
  assumptions: string[]
}

export interface SimulationView {
  id: number
  name: string
  input: CalcInput
  finalAmount: number
  totalInvested: number
  totalInterest: number
  createdAt: string
}

export interface QuizCard {
  id: number
  title: string
  description: string
  category: Topic
  categoryLabel: string
  difficulty: Difficulty
  questionCount: number
  bestPercent: number | null
  attempts: number
}

export interface AttemptHistoryItem {
  attemptId: number
  quizId: number
  quizTitle: string
  category: Topic
  correct: number
  total: number
  percent: number
  finishedAt: string
}

export interface QuizDetail {
  quiz: QuizCard
  myAttempts: AttemptHistoryItem[]
  inProgressAttemptId: number | null
}

export interface QuestionView {
  id: number
  position: number
  statement: string
  options: { id: number; text: string }[]
}

export interface AnswerFeedback {
  questionId: number
  selectedOptionId: number | null
  correct: boolean
  correctOptionId: number
  explanation: string
}

export interface AttemptView {
  attemptId: number
  quizId: number
  quizTitle: string
  status: 'IN_PROGRESS' | 'COMPLETED'
  questions: QuestionView[]
  answered: AnswerFeedback[]
}

export interface CourseCard {
  id: number
  slug: string
  title: string
  summary: string
  category: Topic
  categoryLabel: string
  difficulty: Difficulty
  estimatedMinutes: number
  position: number
  lessonCount: number
  averageRating: number | null
  reviewCount: number
  enrolled: boolean
  completed: boolean
  completedLessons: number
  progressPercent: number
}

export interface AttemptResult {
  attemptId: number
  quizId: number
  quizTitle: string
  correct: number
  total: number
  score: number
  percent: number
  passed: boolean
  finishedAt: string
  review: AnswerFeedback[]
  weakTopics: string[]
  recommendedCourses: CourseCard[]
  reward: Reward
}

export interface LessonSummary {
  id: number
  title: string
  estimatedMinutes: number
  completed: boolean
  hasQuiz: boolean
}

export interface CourseDetail {
  course: CourseCard
  description: string
  objectives: string[]
  modules: { id: number; title: string; lessons: LessonSummary[] }[]
  enrollment: { enrolledAt: string; completedAt: string | null; lastLessonId: number | null; nextLessonId: number | null } | null
  myReview: { rating: number; comment: string | null } | null
}

export interface LessonView {
  id: number
  title: string
  content: string
  estimatedMinutes: number
  courseId: number
  courseTitle: string
  moduleTitle: string
  completed: boolean
  note: string | null
  quizId: number | null
  previousLessonId: number | null
  nextLessonId: number | null
  position: number
  totalLessons: number
}

export interface CompletionResult {
  courseCompleted: boolean
  progressPercent: number
  nextLessonId: number | null
  reward: Reward
}

export interface LearningItem {
  courseId: number
  title: string
  category: Topic
  progressPercent: number
  completedLessons: number
  lessonCount: number
  nextLessonId: number | null
  enrolledAt: string
  completedAt: string | null
}

export interface ReviewView {
  id: number
  author: string
  rating: number
  comment: string | null
  createdAt: string
}

export interface NoteView {
  lessonId: number
  lessonTitle: string
  courseId: number
  courseTitle: string
  content: string | null
  updatedAt: string | null
}

export interface ChallengeView {
  code: string
  title: string
  description: string
  target: number
  progress: number
  points: number
  completed: boolean
}

export interface GamificationSummary {
  totalPoints: number
  level: number
  levelTitle: string
  currentLevelStart: number
  nextLevelStart: number
  currentStreak: number
  bestStreak: number
  weeklyLessonTarget: number
  lessonsThisWeek: number
  achievementsUnlocked: number
  achievementsTotal: number
  achievements: AchievementView[]
  challenges: ChallengeView[]
}

export interface PointEventView {
  type: string
  description: string
  points: number
  createdAt: string
}

export interface NotificationView {
  id: number
  type: string
  title: string
  message: string
  link: string | null
  read: boolean
  createdAt: string
}

export interface ActivityView {
  id: number
  type: string
  description: string
  createdAt: string
}

export interface MonthPoint {
  month: string
  income: number
  expense: number
  net: number
  cumulativeBalance: number
}

export interface Dashboard {
  displayName: string
  goals: { active: number; completed: number; totalSaved: number; totalTarget: number; overallProgress: number; highlights: Goal[] }
  monthFinances: TxSummary
  savingsEvolution: MonthPoint[]
  recentActivity: ActivityView[]
  unreadNotifications: number
  notifications: NotificationView[]
  learning: { enrolled: number; completed: number; inProgress: LearningItem[] }
  quizzes: { attempts: number; averagePercent: number | null; recent: AttemptHistoryItem[] }
  gamification: {
    points: number
    level: number
    levelTitle: string
    currentLevelStart: number
    nextLevelStart: number
    streak: number
    unlocked: number
    total: number
    recentAchievements: AchievementView[]
  }
  recommendations: { title: string; description: string; link: string; kind: string }[]
  hasTransactions: boolean
}

export interface FullReport {
  from: string
  to: string
  finances: {
    income: number
    expense: number
    balance: number
    openingBalance: number
    monthly: MonthPoint[]
    expensesByCategory: CategoryTotal[]
    incomeByCategory: CategoryTotal[]
  }
  goals: {
    active: number
    completed: number
    cancelled: number
    completionRate: number
    contributedInPeriod: number
    withdrawnInPeriod: number
    monthly: { month: string; contributions: number; withdrawals: number }[]
    goals: Goal[]
  }
  quizzes: { attempts: number; averagePercent: number | null; bestPercent: number | null; evolution: { finishedAt: string; quizTitle: string; percent: number }[] }
  courses: { enrolled: number; completedTotal: number; completedInPeriod: number; courses: LearningItem[] }
  achievements: { pointsInPeriod: number; totalPoints: number; unlocked: number; total: number; unlockedInPeriod: AchievementView[] }
  criteria: string[]
}

export interface ChatLink {
  label: string
  path: string
}

export interface ChatResponse {
  answer: string
  topic: string | null
  matched: boolean
  links: ChatLink[]
  suggestions: string[]
  source: string
  disclaimer: string
}

export type ForumCategory = 'QUESTIONS' | 'TIPS' | 'GOALS' | 'STUDIES' | 'INVESTING' | 'PLATFORM'

export interface PostSummary {
  id: number
  category: ForumCategory
  title: string
  excerpt: string
  author: string
  createdAt: string
  edited: boolean
  likes: number
  comments: number
  likedByMe: boolean
  mine: boolean
}

export interface PostDetail {
  post: PostSummary
  body: string
}

export interface CommentView {
  id: number
  author: string
  body: string
  createdAt: string
  edited: boolean
  mine: boolean
}

export interface WalletSummary {
  id: number
  name: string
  description: string | null
  assumedAnnualRate: number
  balance: number
  archived: boolean
  createdAt: string
  simulated: boolean
}

export interface WalletDetail {
  wallet: WalletSummary
  performance: {
    initial: number
    deposits: number
    withdrawals: number
    simulatedReturns: number
    netContributed: number
    balance: number
    returnPercent: number
  }
  evolution: { date: string; balance: number }[]
  disclaimer: string
}

export interface WalletMovement {
  id: number
  type: 'INITIAL' | 'DEPOSIT' | 'WITHDRAWAL' | 'SIMULATED_RETURN'
  amount: number
  occurredOn: string
  note: string | null
  referencePeriod: string | null
  createdAt: string
}

export interface ScenarioResponse {
  startingBalance: number
  months: number
  monthlyContribution: number
  scenarios: { annualRate: number; result: CalcResult }[]
  disclaimer: string
}

export type FeedbackType = 'SUGGESTION' | 'PROBLEM' | 'EXPERIENCE'
export type FeedbackStatus = 'NEW' | 'IN_REVIEW' | 'RESOLVED' | 'ARCHIVED'

export interface FeedbackView {
  id: number
  type: FeedbackType
  subject: string
  message: string
  status: FeedbackStatus
  adminResponse: string | null
  author: string | null
  createdAt: string
  updatedAt: string
}

export interface PublicConfig {
  passwordResetByEmail: boolean
  externalNotifications: boolean
  chatbotProvider: string
  marketData: boolean
}

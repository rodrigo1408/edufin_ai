import type { Difficulty, ForumCategory, GoalCategory, GoalStatus, Role, Topic } from '../api/types'

export const topicLabel: Record<Topic, string> = {
  BUDGET: 'Orçamento',
  CONSCIOUS_CONSUMPTION: 'Consumo consciente',
  SAVING: 'Poupança',
  INTEREST: 'Juros',
  INFLATION: 'Inflação',
  CREDIT: 'Crédito',
  DEBT: 'Endividamento',
  INVESTING: 'Investimentos',
  GOALS: 'Metas',
}

export const difficultyLabel: Record<Difficulty, string> = {
  BEGINNER: 'Iniciante',
  INTERMEDIATE: 'Intermediário',
  ADVANCED: 'Avançado',
}

export const goalCategoryLabel: Record<GoalCategory, string> = {
  ELECTRONICS: 'Eletrônicos',
  EDUCATION: 'Educação',
  TRAVEL: 'Viagem',
  EMERGENCY_FUND: 'Reserva de emergência',
  LEISURE: 'Lazer',
  GIFT: 'Presente',
  CLOTHING: 'Roupas',
  OTHER: 'Outros',
}

export const goalStatusLabel: Record<GoalStatus, string> = {
  ACTIVE: 'Ativa',
  COMPLETED: 'Concluída',
  CANCELLED: 'Cancelada',
}

export const forumCategoryLabel: Record<ForumCategory, string> = {
  QUESTIONS: 'Dúvidas',
  TIPS: 'Dicas',
  GOALS: 'Metas e conquistas',
  STUDIES: 'Estudos',
  INVESTING: 'Investimentos (conceitos)',
  PLATFORM: 'Sobre a plataforma',
}

export const roleLabel: Record<Role, string> = {
  USER: 'Estudante',
  EDUCATOR: 'Educador(a)',
  MODERATOR: 'Moderador(a)',
  ADMIN: 'Administrador(a)',
}

export const originLabel: Record<string, string> = {
  MANUAL: 'Registro',
  SIMULATED: 'Simulado',
  EXTERNAL: 'Integração',
}

export const walletMovementLabel: Record<string, string> = {
  INITIAL: 'Saldo inicial',
  DEPOSIT: 'Aporte',
  WITHDRAWAL: 'Retirada',
  SIMULATED_RETURN: 'Rendimento simulado',
}

export const reportReasonLabel: Record<string, string> = {
  SPAM: 'Spam',
  OFFENSIVE: 'Conteúdo ofensivo',
  PERSONAL_DATA: 'Exposição de dados pessoais',
  SCAM: 'Golpe ou fraude',
  BULLYING: 'Bullying ou assédio',
  OTHER: 'Outro motivo',
}

export const feedbackTypeLabel: Record<string, string> = {
  SUGGESTION: 'Sugestão',
  PROBLEM: 'Problema',
  EXPERIENCE: 'Experiência de uso',
}

export const feedbackStatusLabel: Record<string, string> = {
  NEW: 'Novo',
  IN_REVIEW: 'Em análise',
  RESOLVED: 'Resolvido',
  ARCHIVED: 'Arquivado',
}

export function options<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }))
}

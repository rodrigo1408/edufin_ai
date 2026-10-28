import type { CalcInput } from '../api/types'
import { hasMaxTwoDecimals, parseAmount, todayIso } from './format'

/** Erros de validação por campo: combina regras locais com os erros devolvidos pela API. */
export type Errors = Record<string, string | undefined>

export function hasErrors(errors: Errors): boolean {
  return Object.values(errors).some(Boolean)
}

/** Regras espelhadas do backend (que sempre revalida). Ficam aqui para dar retorno imediato. */
export const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/
export const DISPLAY_NAME_RULE = /^[\p{L}\p{N}_. -]{3,30}$/u

export function validateRegister(f: { email: string; password: string; confirm: string; displayName: string; acceptTerms: boolean; ageConfirmation: boolean }): Errors {
  return {
    email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email) ? undefined : 'Informe um e-mail válido.',
    displayName: DISPLAY_NAME_RULE.test(f.displayName.trim()) ? undefined : 'De 3 a 30 caracteres: letras, números, espaço, ponto, hífen ou _.',
    password: PASSWORD_RULE.test(f.password) ? undefined : 'Use de 8 a 72 caracteres, com letras e números.',
    confirm: f.password === f.confirm ? undefined : 'As senhas não conferem.',
    acceptTerms: f.acceptTerms ? undefined : 'É necessário aceitar os termos.',
    ageConfirmation: f.ageConfirmation ? undefined : 'Confirme esta declaração para continuar.',
  }
}

export function validateGoal(f: { title: string; target: string; deadline: string }): Errors {
  const amount = parseAmount(f.target)
  return {
    title: f.title.trim() ? (f.title.length > 80 ? 'Máximo de 80 caracteres.' : undefined) : 'Dê um nome para a meta.',
    targetAmount: isNaN(amount) || amount <= 0 ? 'Informe um valor maior que zero.' : !hasMaxTwoDecimals(amount) ? 'Use no máximo 2 casas decimais.' : undefined,
    deadline: f.deadline && f.deadline < todayIso() ? 'O prazo não pode estar no passado.' : undefined,
  }
}

export function validateCalc(f: Record<keyof CalcInput, string>): Errors {
  const num = (s: string) => parseAmount(s)
  const months = Number(f.period) * (f.periodUnit === 'YEARS' ? 12 : 1)
  return {
    principal: isNaN(num(f.principal)) || num(f.principal) < 0 ? 'Informe um valor igual ou maior que zero.' : undefined,
    ratePercent: isNaN(num(f.ratePercent)) || num(f.ratePercent) < 0 || num(f.ratePercent) > 100 ? 'A taxa deve estar entre 0% e 100%.' : undefined,
    period: !Number.isInteger(Number(f.period)) || Number(f.period) < 1 ? 'Informe um período inteiro, maior que zero.' : months > 600 ? 'O período máximo é de 50 anos.' : undefined,
    monthlyContribution: isNaN(num(f.monthlyContribution)) || num(f.monthlyContribution) < 0 ? 'Informe um valor igual ou maior que zero.' : undefined,
  }
}

export function describeCalc(i: CalcInput): string {
  return `${i.interestType === 'COMPOUND' ? 'Compostos' : 'Simples'} · ${i.ratePercent}% ${i.rateUnit === 'MONTHLY' ? 'a.m.' : 'a.a.'} · ${i.period} ${i.periodUnit === 'MONTHS' ? 'meses' : 'anos'}`
}

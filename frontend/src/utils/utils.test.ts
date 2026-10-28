import { describe, expect, it } from 'vitest'
import { date, hasMaxTwoDecimals, money, parseAmount, percent } from './format'
import { describeCalc, hasErrors, validateCalc, validateGoal, validateRegister } from './validation'

describe('format', () => {
  it('formata moeda em reais', () => {
    expect(money(1234.5).replace(/\s/g, ' ')).toBe('R$ 1.234,50')
    expect(money(null).replace(/\s/g, ' ')).toBe('R$ 0,00')
  })

  it('formata datas ISO sem deslocar o dia', () => {
    expect(date('2026-03-01')).toBe('01/03/2026')
    expect(date(null)).toBe('—')
  })

  it('formata percentuais', () => {
    expect(percent(33.33)).toBe('33,3%')
    expect(percent(null)).toBe('—')
  })

  it('interpreta valores digitados no padrão brasileiro e internacional', () => {
    expect(parseAmount('1.234,56')).toBe(1234.56)
    expect(parseAmount('10,5')).toBe(10.5)
    expect(parseAmount('99.90')).toBe(99.9)
    expect(parseAmount('abc')).toBeNaN()
    expect(parseAmount('')).toBeNaN()
  })

  it('detecta mais de duas casas decimais', () => {
    expect(hasMaxTwoDecimals(10.25)).toBe(true)
    expect(hasMaxTwoDecimals(10.255)).toBe(false)
  })
})

describe('validation', () => {
  it('valida cadastro campo a campo', () => {
    const errors = validateRegister({ email: 'x', password: '123', confirm: '456', displayName: 'a', acceptTerms: false, ageConfirmation: true })
    expect(errors.email).toBeDefined()
    expect(errors.password).toBeDefined()
    expect(errors.confirm).toBeDefined()
    expect(errors.displayName).toBeDefined()
    expect(errors.acceptTerms).toBeDefined()
    expect(errors.ageConfirmation).toBeUndefined()
    const ok = validateRegister({ email: 'a@b.com', password: 'Senha123', confirm: 'Senha123', displayName: 'Ana_10', acceptTerms: true, ageConfirmation: true })
    expect(hasErrors(ok)).toBe(false)
  })

  it('valida metas', () => {
    expect(validateGoal({ title: '', target: '0', deadline: '' }).title).toBeDefined()
    expect(validateGoal({ title: 'Fone', target: '0', deadline: '' }).targetAmount).toBeDefined()
    expect(validateGoal({ title: 'Fone', target: '10,123', deadline: '' }).targetAmount).toContain('2 casas')
    expect(validateGoal({ title: 'Fone', target: '100', deadline: '2000-01-01' }).deadline).toBeDefined()
    expect(hasErrors(validateGoal({ title: 'Fone', target: '350,00', deadline: '' }))).toBe(false)
  })

  it('valida parâmetros da calculadora', () => {
    const base = { interestType: 'COMPOUND', principal: '100', ratePercent: '1', rateUnit: 'MONTHLY', period: '12', periodUnit: 'MONTHS', monthlyContribution: '0' }
    expect(hasErrors(validateCalc(base))).toBe(false)
    expect(validateCalc({ ...base, ratePercent: '150' }).ratePercent).toBeDefined()
    expect(validateCalc({ ...base, period: '51', periodUnit: 'YEARS' }).period).toContain('50 anos')
    expect(validateCalc({ ...base, principal: '-1' }).principal).toBeDefined()
  })

  it('descreve cenários', () => {
    expect(describeCalc({ interestType: 'SIMPLE', principal: 1, ratePercent: 2, rateUnit: 'YEARLY', period: 3, periodUnit: 'YEARS', monthlyContribution: 0 })).toBe('Simples · 2% a.a. · 3 anos')
  })
})

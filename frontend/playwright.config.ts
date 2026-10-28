import { defineConfig, devices } from '@playwright/test'

/**
 * Testes ponta a ponta contra a aplicação REAL em execução (backend + SPA servido pelo Spring).
 * Suba a aplicação antes (ver README) ou informe E2E_BASE_URL. Usa o Chrome instalado na máquina
 * (channel "chrome"); para usar o Chromium do Playwright, rode "npx playwright install chromium"
 * e defina E2E_CHANNEL=chromium.
 */
const channel = process.env.E2E_CHANNEL === 'chromium' ? undefined : (process.env.E2E_CHANNEL ?? 'chrome')

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], channel }, testIgnore: /mobile.spec.ts/ },
    { name: 'mobile', use: { ...devices['Pixel 7'], channel }, testMatch: /mobile\.spec\.ts/ },
  ],
})

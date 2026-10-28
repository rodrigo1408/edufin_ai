// Copia o build do frontend (dist/) para backend/src/main/resources/static, permitindo que o
// Spring Boot sirva o SPA na mesma origem da API (um único processo/jar).
import { cpSync, existsSync, rmSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const dist = resolve(here, '../dist')
const target = resolve(here, '../../backend/src/main/resources/static')

if (!existsSync(dist)) {
  console.error('dist/ não encontrado. Rode "npm run build" antes.')
  process.exit(1)
}
rmSync(target, { recursive: true, force: true })
cpSync(dist, target, { recursive: true })
console.log(`Frontend copiado para ${target}`)

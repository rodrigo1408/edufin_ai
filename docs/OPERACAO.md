# Operação — implantação, backup e monitoramento

## Implantação
1. Provisionar PostgreSQL 17 (ou usar o `docker-compose.yml`).
2. Definir as variáveis de ambiente (ver `.env.example`), incluindo `DB_*`, `ADMIN_*`, `APP_PUBLIC_URL`, `COOKIE_SECURE=true`.
3. Construir a imagem: `docker build -t edufin:1.0.0 .` (o Dockerfile compila o frontend, embute no jar e gera uma imagem só com JRE, rodando como usuário não-root).
4. Executar atrás de um proxy HTTPS (Nginx, Caddy, balanceador gerenciado). Encaminhar apenas a porta 8080.
5. Na primeira subida, o Flyway aplica as migrações e o administrador inicial é criado. Troque a senha dele pelo perfil.

Sem Docker: `npm run build:backend` no frontend, `mvn package` no backend e `java -jar edufin-backend-1.0.0.jar`.

## Migrações
- Aplicadas automaticamente na inicialização (Flyway). Nunca edite migrações já aplicadas; crie `V{n}__descricao.sql`.
- O Hibernate valida o mapeamento; divergências impedem a subida (falha rápida, sem alteração destrutiva).

## Backup e restauração (PostgreSQL)
```bash
# backup diário (exemplo de cron), com retenção de 14 dias
pg_dump -Fc -h HOST -U edufin edufin > edufin-$(date +%F).dump
find /backups -name 'edufin-*.dump' -mtime +14 -delete
# restauração
pg_restore -c -d edufin -h HOST -U edufin edufin-2026-10-28.dump
```
Com Docker Compose: `docker compose exec db pg_dump -Fc -U edufin edufin > backup.dump`.
Teste a restauração periodicamente. Backups contêm dados pessoais: armazene-os criptografados e com acesso restrito.
No perfil `local` (H2), basta copiar a pasta `backend/data/` com a aplicação parada.

## Monitoramento
- Saúde: `GET /actuator/health` (usado no `HEALTHCHECK` da imagem).
- Logs em stdout (formato padrão do Spring Boot): eventos de segurança relevantes (criação de conta, mudança de papel, desativação, moderação, redefinição de senha) registram somente ids.

## Jobs agendados
| Job | Padrão | Variável |
|---|---|---|
| Lembretes (prazos de metas, estudo) | 08:00 diário | `REMINDERS_CRON` |
| Retenção de dados | 03:30 diário | `RETENTION_CRON` |

Use `-` para desativar um job (ex.: em réplicas adicionais, para não duplicar execuções).

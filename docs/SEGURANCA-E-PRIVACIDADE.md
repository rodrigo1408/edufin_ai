# Segurança, privacidade e proteção de menores — EduFin

## Autenticação e sessão
- Senhas com **BCrypt** (encoder delegável, permite migrar de algoritmo); política: 8–72 caracteres com letras e números.
- Sessão no servidor com cookie `EDUFIN_SESSION` **HttpOnly**, `SameSite=Lax` e `Secure` configurável (`COOKIE_SECURE=true` em HTTPS); expiração por inatividade (`SESSION_TIMEOUT`).
- Troca do id de sessão no login (anti *session fixation*).
- **Carimbo de segurança**: trocar senha, papel ou desativar a conta invalida todas as sessões abertas; o filtro `SessionUserRefreshFilter` revalida o usuário a cada requisição.
- Mensagem de login genérica ("E-mail ou senha inválidos") — não revela se o e-mail existe; a solicitação de recuperação também responde igual para e-mails existentes ou não.
- Tokens de redefinição: 32 bytes aleatórios, apenas o **hash SHA-256** é armazenado, uso único, 30 minutos.

## Autorização e isolamento de dados
- Permissões por papel verificadas no servidor (URL) e posse verificada nos serviços (`findByIdAndUserId`). Recursos de outro usuário retornam 404.
- Proteções contra elevação de privilégio: o cadastro ignora qualquer campo `role`; administradores não alteram o próprio papel; o último administrador ativo não pode ser rebaixado/desativado/excluído.
- Administradores **não têm endpoints para ver dados financeiros** de usuários.

## Proteções da aplicação
- **CSRF** (cookie + cabeçalho) em todas as requisições que alteram dados.
- **CORS** fechado por padrão (mesma origem); origens extras só via `CORS_ALLOWED_ORIGINS`.
- Cabeçalhos: CSP restritiva (`script-src 'self'`, `frame-ancestors 'none'`), `X-Frame-Options: DENY`, `X-Content-Type-Options`, `Referrer-Policy: same-origin`.
- Validação de entrada com Bean Validation nos DTOs + regras de negócio nos serviços; limites de tamanho em todos os textos.
- **Injeção de SQL**: apenas JPQL/Criteria parametrizados; buscas `LIKE` com escape de curingas.
- **XSS**: o frontend nunca usa HTML bruto; conteúdo de aulas é renderizado por um parser Markdown que gera elementos React.
- **Rate limiting** (em memória): login por IP e por e-mail (5 tentativas/5 min por conta), cadastro por IP, recuperação de senha, assistente, publicações, comentários, denúncias, feedback e exportação.
- Erros sem detalhes internos (sem *stack trace*); erros inesperados são registrados no log do servidor.
- Logs sem senhas, tokens, e-mails em claro ou valores financeiros: registram apenas ids e tipos de evento.
- Segredos somente por variáveis de ambiente; `.env` está no `.gitignore`.

## LGPD e proteção de menores de idade
A plataforma é para todas as idades a partir de 12 anos; como pode ser usada por menores, segue também as regras de proteção. Base: LGPD (Lei 13.709/2018), em especial o art. 14 (melhor interesse de crianças e adolescentes), e o ECA.
- **Minimização**: coletamos apenas e-mail, apelido e senha. Não pedimos nome completo, CPF, telefone, endereço, escola ou data de nascimento. O apelido é público; o e-mail nunca é exibido a outros usuários.
- **Transparência e ciência do responsável**: o cadastro exige aceite dos termos e declaração de idade mínima (12 anos) e de ciência do(a) responsável para menores de 18 anos.
  - **Pendência jurídica:** para crianças (menores de 12 anos) a LGPD exige consentimento específico de um dos pais. A plataforma não oferece fluxo verificável de consentimento parental, por isso não se destina a menores de 12 anos. Os textos dos termos e da política de privacidade devem ser redigidos/revisados por profissional jurídico antes da publicação.
- **Direitos do titular**: acesso/portabilidade (`GET /api/me/export`), correção (perfil), eliminação (`DELETE /api/me` — remoção em cascata; feedbacks permanecem anonimizados).
- **Não exposição financeira**: dados financeiros são privados; nada é publicado automaticamente; a gamificação não usa valores monetários e não há ranking.
- **Retenção**: histórico de atividades (padrão 365 dias), notificações lidas (180 dias) e tokens expirados são apagados por job diário. O assistente não armazena conversas.
- **Comunidade segura (inclusive para menores de idade)**: bloqueio automático de telefone, e-mail e CPF (reduz aliciamento e exposição), filtro de termos ofensivos e de promessas de "dinheiro fácil", limite de links, denúncia com ocultação automática após 3 denúncias, moderação por papel dedicado e notificação ao autor quando o conteúdo é moderado.

## Riscos residuais e recomendações para produção
- Ativar HTTPS (proxy reverso) e `COOKIE_SECURE=true`; configurar `server.forward-headers-strategy` se houver proxy, para o rate limit usar o IP real.
- Sessões e rate limit em memória: para mais de uma instância, usar Spring Session JDBC/Redis e limitador compartilhado.
- A lista de termos bloqueados é básica; a moderação humana continua necessária.
- Considerar verificação de e-mail no cadastro quando houver SMTP.
- Revisar `API_DOCS_ENABLED=false` em produção, se a documentação pública não for desejada.

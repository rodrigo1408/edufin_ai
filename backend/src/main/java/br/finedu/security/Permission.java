package br.finedu.security;

/** Permissões granulares verificadas no servidor. Cada papel agrega um conjunto delas. */
public enum Permission {
    /** Acesso à área administrativa (qualquer papel além de USER). */
    ADMIN_AREA,
    /** Criar e editar cursos, aulas e quizzes. */
    CONTENT_MANAGE,
    /** Moderar publicações, comentários e avaliações. */
    FORUM_MODERATE,
    /** Consultar e tratar feedbacks dos usuários. */
    FEEDBACK_MANAGE,
    /** Gerenciar contas e papéis. */
    USER_MANAGE
}

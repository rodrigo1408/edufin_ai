package br.finedu.gamification;

/**
 * Regras de pontuação. A chave de referência de cada evento é única por usuário e tipo
 * (restrição no banco), o que torna a concessão idempotente:
 * <ul>
 *   <li>Regras "por item" usam o id do item (uma aula só pontua uma vez).</li>
 *   <li>Regras de hábito usam a data (no máximo uma pontuação por dia), evitando que criar e
 *       cancelar metas ou lançar transações repetidamente gere pontos.</li>
 * </ul>
 * Nenhuma regra depende de valores monetários: a gamificação recompensa aprendizado e hábitos,
 * não quanto dinheiro a pessoa tem.
 */
public enum PointRule {
    LESSON_COMPLETED(10, "Aula concluída"),
    COURSE_COMPLETED(50, "Curso concluído"),
    QUIZ_FINISHED(0, "Quiz finalizado"),
    QUIZ_FIRST_PASS(15, "Quiz aprovado (70% ou mais) pela primeira vez"),
    QUIZ_PERFECT(10, "Quiz com 100% de acertos"),
    GOAL_CREATED(5, "Meta criada (hábito diário)"),
    GOAL_COMPLETED(20, "Meta concluída (hábito diário)"),
    TRANSACTION_LOGGED(2, "Registro de movimentação (hábito diário)"),
    SIMULATION_SAVED(3, "Simulação salva (hábito diário)"),
    WALLET_CREATED(5, "Carteira simulada criada (hábito diário)"),
    ACHIEVEMENT_UNLOCKED(0, "Conquista desbloqueada"),
    CHALLENGE_COMPLETED(0, "Desafio semanal concluído");

    private final int points;
    private final String description;

    PointRule(int points, String description) {
        this.points = points;
        this.description = description;
    }

    public int points() {
        return points;
    }

    public String description() {
        return description;
    }
}

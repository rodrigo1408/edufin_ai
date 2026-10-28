package br.finedu.gamification;

/**
 * Desafios semanais (semana ISO, segunda a domingo, no fuso da aplicação). O progresso é
 * calculado a partir dos eventos de pontos da semana; a recompensa é concedida uma única vez
 * por semana (chave "CODIGO:semana").
 */
public enum WeeklyChallenge {
    WEEK_LESSONS("Maratona do saber", "Conclua 3 aulas nesta semana.", PointRule.LESSON_COMPLETED, 3, 30),
    WEEK_QUIZZES("Teste seus conhecimentos", "Finalize 2 quizzes nesta semana.", PointRule.QUIZ_FINISHED, 2, 20),
    WEEK_BUDGET("Diário financeiro", "Registre movimentações em 3 dias diferentes nesta semana.",
            PointRule.TRANSACTION_LOGGED, 3, 20);

    private final String title;
    private final String description;
    private final PointRule metric;
    private final int target;
    private final int points;

    WeeklyChallenge(String title, String description, PointRule metric, int target, int points) {
        this.title = title;
        this.description = description;
        this.metric = metric;
        this.target = target;
        this.points = points;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public PointRule metric() {
        return metric;
    }

    public int target() {
        return target;
    }

    public int points() {
        return points;
    }
}

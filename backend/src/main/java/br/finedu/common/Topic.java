package br.finedu.common;

/** Temas educativos compartilhados por cursos, quizzes, fórum e recomendações. */
public enum Topic {
    BUDGET("Orçamento pessoal"),
    CONSCIOUS_CONSUMPTION("Consumo consciente"),
    SAVING("Poupança e reserva"),
    INTEREST("Juros"),
    INFLATION("Inflação"),
    CREDIT("Crédito"),
    DEBT("Endividamento"),
    INVESTING("Fundamentos de investimentos"),
    GOALS("Metas financeiras");

    private final String label;

    Topic(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

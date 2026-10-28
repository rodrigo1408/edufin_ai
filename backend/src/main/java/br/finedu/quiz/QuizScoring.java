package br.finedu.quiz;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de pontuação do quiz: 10 pontos por acerto; percentual = acertos / total de perguntas
 * (perguntas sem resposta contam como erro); aprovação a partir de 70%.
 */
public final class QuizScoring {

    public static final int POINTS_PER_CORRECT = 10;
    public static final BigDecimal PASS_PERCENT = new BigDecimal("70.00");

    public record Score(int correct, int total, int points, BigDecimal percent) {
        public boolean passed() {
            return percent.compareTo(PASS_PERCENT) >= 0;
        }

        public boolean perfect() {
            return total > 0 && correct == total;
        }
    }

    private QuizScoring() {
    }

    public static Score score(int correct, int total) {
        if (total <= 0) {
            return new Score(0, 0, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
        }
        if (correct < 0 || correct > total) {
            throw new IllegalArgumentException("Número de acertos inválido");
        }
        BigDecimal percent = BigDecimal.valueOf(correct * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN);
        return new Score(correct, total, correct * POINTS_PER_CORRECT, percent);
    }
}

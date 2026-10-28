package br.finedu.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QuizScoringTest {

    @Test
    void computesPointsAndPercent() {
        QuizScoring.Score s = QuizScoring.score(3, 4);
        assertThat(s.points()).isEqualTo(30);
        assertThat(s.percent()).isEqualByComparingTo("75.00");
        assertThat(s.passed()).isTrue();
        assertThat(s.perfect()).isFalse();
    }

    @Test
    void passThresholdIsSeventyPercent() {
        assertThat(QuizScoring.score(7, 10).passed()).isTrue();
        assertThat(QuizScoring.score(2, 3).passed()).isFalse(); // 66,67%
        assertThat(QuizScoring.score(2, 3).percent()).isEqualByComparingTo("66.67");
    }

    @Test
    void perfectScore() {
        assertThat(QuizScoring.score(5, 5).perfect()).isTrue();
        assertThat(QuizScoring.score(5, 5).percent()).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsImpossibleValues() {
        assertThatThrownBy(() -> QuizScoring.score(6, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuizScoring.score(-1, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(QuizScoring.score(0, 0).percent()).isEqualByComparingTo("0");
    }
}

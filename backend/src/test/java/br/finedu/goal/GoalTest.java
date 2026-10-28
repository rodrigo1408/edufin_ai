package br.finedu.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.finedu.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GoalTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private Goal goal(String target) {
        return new Goal(1L, "Fone", null, Goal.Category.ELECTRONICS, new BigDecimal(target), null, NOW);
    }

    @Test
    void completesAutomaticallyWhenBalanceReachesTarget() {
        Goal g = goal("100.00");
        assertThat(g.refreshStatus(new BigDecimal("99.99"), NOW)).isFalse();
        assertThat(g.getStatus()).isEqualTo(Goal.Status.ACTIVE);
        assertThat(g.refreshStatus(new BigDecimal("100.00"), NOW)).isTrue();
        assertThat(g.getStatus()).isEqualTo(Goal.Status.COMPLETED);
        assertThat(g.getCompletedAt()).isEqualTo(NOW);
        // Já concluída: não "conclui" de novo
        assertThat(g.refreshStatus(new BigDecimal("150.00"), NOW)).isFalse();
    }

    @Test
    void reopensWhenBalanceDropsBelowTarget() {
        Goal g = goal("100.00");
        g.refreshStatus(new BigDecimal("100.00"), NOW);
        g.refreshStatus(new BigDecimal("40.00"), NOW);
        assertThat(g.getStatus()).isEqualTo(Goal.Status.ACTIVE);
        assertThat(g.getCompletedAt()).isNull();
    }

    @Test
    void cancelledGoalIsFrozen() {
        Goal g = goal("100.00");
        g.cancel(NOW);
        assertThat(g.getStatus()).isEqualTo(Goal.Status.CANCELLED);
        assertThat(g.refreshStatus(new BigDecimal("500"), NOW)).isFalse();
        assertThatThrownBy(g::requireNotCancelled).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> g.cancel(NOW)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> g.update("x", null, Goal.Category.OTHER, BigDecimal.TEN, null, NOW))
                .isInstanceOf(ApiException.class);
    }
}

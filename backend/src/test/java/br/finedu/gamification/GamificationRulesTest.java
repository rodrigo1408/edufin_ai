package br.finedu.gamification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GamificationRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);

    @Test
    void levelThresholds() {
        assertThat(GamificationRules.levelFor(0).level()).isEqualTo(1);
        assertThat(GamificationRules.levelFor(99).level()).isEqualTo(1);
        assertThat(GamificationRules.levelFor(100).level()).isEqualTo(2);
        assertThat(GamificationRules.levelFor(299).level()).isEqualTo(2);
        assertThat(GamificationRules.levelFor(300).level()).isEqualTo(3);
        assertThat(GamificationRules.levelFor(1000).level()).isEqualTo(5);
        assertThat(GamificationRules.levelFor(100_000).title()).isEqualTo("Mestre das Finanças");
        GamificationRules.Level l = GamificationRules.levelFor(150);
        assertThat(l.currentLevelStart()).isEqualTo(100);
        assertThat(l.nextLevelStart()).isEqualTo(300);
    }

    @Test
    void streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        Set<LocalDate> days = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(5));
        GamificationRules.Streak s = GamificationRules.streak(days, TODAY);
        assertThat(s.current()).isEqualTo(3);
        assertThat(s.best()).isEqualTo(3);

        GamificationRules.Streak yesterday = GamificationRules.streak(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
        assertThat(yesterday.current()).isEqualTo(2);

        GamificationRules.Streak broken = GamificationRules.streak(Set.of(TODAY.minusDays(3)), TODAY);
        assertThat(broken.current()).isZero();
        assertThat(broken.best()).isEqualTo(1);
        assertThat(GamificationRules.streak(Set.of(), TODAY)).isEqualTo(new GamificationRules.Streak(0, 0));
    }

    @Test
    void achievementRulesDependOnRealCounts() {
        GamificationRules.Stats none = new GamificationRules.Stats(Map.of(), 0);
        GamificationRules.Stats some = new GamificationRules.Stats(Map.of(
                PointRule.LESSON_COMPLETED, 1L, PointRule.TRANSACTION_LOGGED, 7L, PointRule.QUIZ_FIRST_PASS, 2L), 3);
        assertThat(GamificationRules.isSatisfied("FIRST_LESSON", none)).isFalse();
        assertThat(GamificationRules.isSatisfied("FIRST_LESSON", some)).isTrue();
        assertThat(GamificationRules.isSatisfied("BUDGET_TRACKER", some)).isTrue();
        assertThat(GamificationRules.isSatisfied("QUIZ_PASS_3", some)).isFalse();
        assertThat(GamificationRules.isSatisfied("STREAK_3", some)).isTrue();
        assertThat(GamificationRules.isSatisfied("STREAK_7", some)).isFalse();
        assertThat(GamificationRules.isSatisfied("UNKNOWN_CODE", some)).isFalse();
    }

    @Test
    void noRuleRewardsMoneyAmounts() {
        for (PointRule rule : PointRule.values()) {
            assertThat(rule.name()).doesNotContain("BALANCE").doesNotContain("AMOUNT");
        }
    }
}

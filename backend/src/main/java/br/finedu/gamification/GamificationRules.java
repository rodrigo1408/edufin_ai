package br.finedu.gamification;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Regras puras (sem banco) de níveis, sequências e conquistas — facilmente testáveis. */
public final class GamificationRules {

    public static final String[] LEVEL_TITLES = {
            "Iniciante", "Aprendiz", "Explorador(a)", "Planejador(a)", "Estrategista", "Mestre das Finanças"
    };

    /** Estatísticas usadas pelas regras de conquistas. */
    public record Stats(Map<PointRule, Long> counts, int bestStreak) {
        long count(PointRule rule) {
            return counts.getOrDefault(rule, 0L);
        }
    }

    public record Level(int level, String title, long currentLevelStart, long nextLevelStart) {
    }

    public record Streak(int current, int best) {
    }

    private static final Map<String, Predicate<Stats>> ACHIEVEMENTS = Map.ofEntries(
            Map.entry("FIRST_LESSON", s -> s.count(PointRule.LESSON_COMPLETED) >= 1),
            Map.entry("FIRST_COURSE", s -> s.count(PointRule.COURSE_COMPLETED) >= 1),
            Map.entry("THREE_COURSES", s -> s.count(PointRule.COURSE_COMPLETED) >= 3),
            Map.entry("FIRST_QUIZ", s -> s.count(PointRule.QUIZ_FINISHED) >= 1),
            Map.entry("QUIZ_PASS_3", s -> s.count(PointRule.QUIZ_FIRST_PASS) >= 3),
            Map.entry("QUIZ_PERFECT", s -> s.count(PointRule.QUIZ_PERFECT) >= 1),
            Map.entry("FIRST_GOAL", s -> s.count(PointRule.GOAL_CREATED) >= 1),
            Map.entry("GOAL_ACHIEVED", s -> s.count(PointRule.GOAL_COMPLETED) >= 1),
            Map.entry("BUDGET_TRACKER", s -> s.count(PointRule.TRANSACTION_LOGGED) >= 7),
            Map.entry("FIRST_SIMULATION", s -> s.count(PointRule.SIMULATION_SAVED) >= 1),
            Map.entry("FIRST_WALLET", s -> s.count(PointRule.WALLET_CREATED) >= 1),
            Map.entry("STREAK_3", s -> s.bestStreak() >= 3),
            Map.entry("STREAK_7", s -> s.bestStreak() >= 7),
            Map.entry("CHALLENGE_1", s -> s.count(PointRule.CHALLENGE_COMPLETED) >= 1));

    private GamificationRules() {
    }

    public static boolean isSatisfied(String achievementCode, Stats stats) {
        Predicate<Stats> rule = ACHIEVEMENTS.get(achievementCode);
        return rule != null && rule.test(stats);
    }

    /** XP necessário para iniciar o nível n: 50·n·(n−1) → 0, 100, 300, 600, 1000, 1500... */
    public static long levelStart(int level) {
        return 50L * level * (level - 1);
    }

    public static Level levelFor(long xp) {
        int level = 1;
        while (levelStart(level + 1) <= xp) {
            level++;
        }
        String title = LEVEL_TITLES[Math.min(level - 1, LEVEL_TITLES.length - 1)];
        return new Level(level, title, levelStart(level), levelStart(level + 1));
    }

    /**
     * Sequência de dias com atividade de aprendizado. A sequência atual continua válida se a última
     * atividade foi hoje ou ontem (o dia de hoje ainda pode ser cumprido).
     */
    public static Streak streak(Set<LocalDate> activeDays, LocalDate today) {
        if (activeDays.isEmpty()) {
            return new Streak(0, 0);
        }
        TreeSet<LocalDate> days = new TreeSet<>(activeDays);
        int best = 0;
        int run = 0;
        LocalDate prev = null;
        for (LocalDate d : days) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        int current = 0;
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        while (days.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        return new Streak(current, best);
    }
}

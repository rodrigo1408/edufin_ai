package br.finedu.gamification;

import br.finedu.common.AppTime;
import br.finedu.common.PageResponse;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import br.finedu.user.UserPreferencesRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concede pontos, conquistas e desafios. Toda validação acontece aqui, no servidor; o cliente
 * apenas recebe o resultado. Executa dentro da transação do caso de uso que originou o evento.
 */
@Service
public class GamificationService {

    public record AchievementView(String code, String title, String description, String icon, int points,
                                  boolean unlocked, Instant unlockedAt) {
    }

    public record Reward(int points, List<AchievementView> achievements, List<String> challenges) {
        public static Reward none() {
            return new Reward(0, List.of(), List.of());
        }

        public Reward plus(Reward other) {
            List<AchievementView> a = new ArrayList<>(achievements);
            a.addAll(other.achievements);
            List<String> c = new ArrayList<>(challenges);
            c.addAll(other.challenges);
            return new Reward(points + other.points, a, c);
        }
    }

    public record ChallengeView(String code, String title, String description, int target, long progress,
                                int points, boolean completed) {
    }

    public record PointEventView(String type, String description, int points, Instant createdAt) {
    }

    public record Summary(long totalPoints, int level, String levelTitle, long currentLevelStart,
                          long nextLevelStart, int currentStreak, int bestStreak, int weeklyLessonTarget,
                          long lessonsThisWeek, long achievementsUnlocked, long achievementsTotal,
                          List<AchievementView> achievements, List<ChallengeView> challenges) {
    }

    private static final Set<PointRule> LEARNING_EVENTS = Set.of(PointRule.LESSON_COMPLETED, PointRule.QUIZ_FINISHED);

    private final PointEventRepository events;
    private final AchievementRepository achievements;
    private final UserAchievementRepository userAchievements;
    private final UserPreferencesRepository preferences;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final AppTime time;

    public GamificationService(PointEventRepository events, AchievementRepository achievements,
                               UserAchievementRepository userAchievements, UserPreferencesRepository preferences,
                               NotificationService notifications, ActivityService activity, AppTime time) {
        this.events = events;
        this.achievements = achievements;
        this.userAchievements = userAchievements;
        this.preferences = preferences;
        this.notifications = notifications;
        this.activity = activity;
        this.time = time;
    }

    /** Pontua uma ação vinculada a um item (ex.: id da aula). Repetições não geram novos pontos. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reward award(Long userId, PointRule rule, String referenceKey) {
        return award(userId, rule, referenceKey, rule.points(), rule.description());
    }

    /** Pontua um hábito no máximo uma vez por dia (chave = data no fuso da aplicação). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reward awardDaily(Long userId, PointRule rule) {
        return award(userId, rule, time.today().toString());
    }

    private Reward award(Long userId, PointRule rule, String referenceKey, int points, String description) {
        if (events.existsByUserIdAndEventTypeAndReferenceKey(userId, rule, referenceKey)) {
            return Reward.none();
        }
        events.save(new PointEvent(userId, rule, referenceKey, points, description, time.now()));
        Reward base = new Reward(points, List.of(), List.of());
        if (rule == PointRule.ACHIEVEMENT_UNLOCKED || rule == PointRule.CHALLENGE_COMPLETED) {
            return base;
        }
        return base.plus(evaluateChallenges(userId)).plus(evaluateAchievements(userId));
    }

    private Reward evaluateChallenges(Long userId) {
        Reward reward = Reward.none();
        Instant weekStart = time.startOfDay(time.startOfWeek());
        String week = time.isoWeekKey();
        for (WeeklyChallenge c : WeeklyChallenge.values()) {
            String key = c.name() + ":" + week;
            if (events.existsByUserIdAndEventTypeAndReferenceKey(userId, PointRule.CHALLENGE_COMPLETED, key)) {
                continue;
            }
            long progress = events.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(userId, c.metric(), weekStart);
            if (progress >= c.target()) {
                events.save(new PointEvent(userId, PointRule.CHALLENGE_COMPLETED, key, c.points(),
                        "Desafio semanal: " + c.title(), time.now()));
                notifications.notify(userId, NotificationType.CHALLENGE_COMPLETED, "Desafio concluído!",
                        "Você completou o desafio \"" + c.title() + "\" e ganhou " + c.points() + " pontos.",
                        "/gamificacao", "CHALLENGE:" + key);
                reward = reward.plus(new Reward(c.points(), List.of(), List.of(c.title())));
            }
        }
        return reward;
    }

    private Reward evaluateAchievements(Long userId) {
        Set<String> unlocked = new HashSet<>(userAchievements.unlockedCodes(userId));
        GamificationRules.Stats stats = null;
        Reward reward = Reward.none();
        for (Achievement a : achievements.findAllByOrderByIdAsc()) {
            if (unlocked.contains(a.getCode())) {
                continue;
            }
            if (stats == null) {
                stats = stats(userId);
            }
            if (GamificationRules.isSatisfied(a.getCode(), stats)) {
                Instant now = time.now();
                userAchievements.save(new UserAchievement(userId, a, now));
                events.save(new PointEvent(userId, PointRule.ACHIEVEMENT_UNLOCKED, a.getCode(), a.getPoints(),
                        "Conquista: " + a.getTitle(), now));
                notifications.notify(userId, NotificationType.ACHIEVEMENT_UNLOCKED, "Nova conquista: " + a.getTitle(),
                        a.getDescription(), "/gamificacao", "ACHIEVEMENT:" + a.getCode());
                activity.record(userId, ActivityLog.Type.ACHIEVEMENT_UNLOCKED, "Conquista desbloqueada: " + a.getTitle());
                reward = reward.plus(new Reward(a.getPoints(),
                        List.of(view(a, true, now)), List.of()));
            }
        }
        return reward;
    }

    private GamificationRules.Stats stats(Long userId) {
        Map<PointRule, Long> counts = new EnumMap<>(PointRule.class);
        for (Object[] row : events.countsByType(userId)) {
            counts.put((PointRule) row[0], (Long) row[1]);
        }
        return new GamificationRules.Stats(counts, streak(userId).best());
    }

    private GamificationRules.Streak streak(Long userId) {
        Set<LocalDate> days = events.activityInstants(userId, LEARNING_EVENTS).stream()
                .map(time::toDate).collect(Collectors.toSet());
        return GamificationRules.streak(days, time.today());
    }

    @Transactional(readOnly = true)
    public Summary summary(Long userId) {
        long xp = events.totalPoints(userId);
        GamificationRules.Level level = GamificationRules.levelFor(xp);
        GamificationRules.Streak streak = streak(userId);
        Instant weekStart = time.startOfDay(time.startOfWeek());
        String week = time.isoWeekKey();

        Map<String, Instant> unlockedAt = userAchievements.findByUser(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getCode(), UserAchievement::getUnlockedAt));
        List<AchievementView> all = achievements.findAllByOrderByIdAsc().stream()
                .map(a -> view(a, unlockedAt.containsKey(a.getCode()), unlockedAt.get(a.getCode())))
                .toList();

        List<ChallengeView> challenges = new ArrayList<>();
        for (WeeklyChallenge c : WeeklyChallenge.values()) {
            long progress = events.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(userId, c.metric(), weekStart);
            boolean done = events.existsByUserIdAndEventTypeAndReferenceKey(userId, PointRule.CHALLENGE_COMPLETED,
                    c.name() + ":" + week);
            challenges.add(new ChallengeView(c.name(), c.title(), c.description(), c.target(),
                    Math.min(progress, c.target()), c.points(), done));
        }
        long lessonsThisWeek = events.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(userId,
                PointRule.LESSON_COMPLETED, weekStart);
        int target = preferences.getOrDefault(userId).getWeeklyLessonTarget();
        return new Summary(xp, level.level(), level.title(), level.currentLevelStart(), level.nextLevelStart(),
                streak.current(), streak.best(), target, lessonsThisWeek, unlockedAt.size(), all.size(), all,
                challenges);
    }

    @Transactional(readOnly = true)
    public PageResponse<PointEventView> history(Long userId, int page, int size) {
        return PageResponse.of(events.findByUserIdAndPointsGreaterThanOrderByCreatedAtDescIdDesc(userId, 0,
                        PageRequest.of(page, size)),
                e -> new PointEventView(e.getEventType().name(), e.getDescription(), e.getPoints(), e.getCreatedAt()));
    }

    private static AchievementView view(Achievement a, boolean unlocked, Instant at) {
        return new AchievementView(a.getCode(), a.getTitle(), a.getDescription(), a.getIcon(), a.getPoints(),
                unlocked, at);
    }
}

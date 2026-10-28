package br.finedu.dashboard;

import br.finedu.common.AppTime;
import br.finedu.common.Money;
import br.finedu.common.Topic;
import br.finedu.course.CourseDtos.CourseCard;
import br.finedu.course.CourseDtos.LearningItem;
import br.finedu.course.CourseService;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.AchievementView;
import br.finedu.goal.Goal;
import br.finedu.goal.GoalDtos.GoalView;
import br.finedu.goal.GoalService;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationService.NotificationView;
import br.finedu.quiz.QuizAttempt;
import br.finedu.quiz.QuizAttemptRepository;
import br.finedu.quiz.QuizDtos.AttemptHistoryItem;
import br.finedu.quiz.QuizService;
import br.finedu.report.ReportService;
import br.finedu.report.ReportService.MonthPoint;
import br.finedu.transaction.TransactionDtos.Summary;
import br.finedu.transaction.TransactionRepository;
import br.finedu.transaction.TransactionService;
import br.finedu.user.ActivityLogRepository;
import br.finedu.user.UserDtos.ActivityView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel inicial: todos os indicadores vêm de dados persistidos do próprio usuário. */
@Service
public class DashboardService {

    public record GoalsSummary(int active, int completed, BigDecimal totalSaved, BigDecimal totalTarget,
                               BigDecimal overallProgress, List<GoalView> highlights) {
    }

    public record LearningSummary(int enrolled, int completed, List<LearningItem> inProgress) {
    }

    public record QuizSummary(int attempts, BigDecimal averagePercent, List<AttemptHistoryItem> recent) {
    }

    public record GamificationSnapshot(long points, int level, String levelTitle, long currentLevelStart,
                                       long nextLevelStart, int streak, long unlocked, long total,
                                       List<AchievementView> recentAchievements) {
    }

    public record Recommendation(String title, String description, String link, String kind) {
    }

    public record Dashboard(String displayName, GoalsSummary goals, Summary monthFinances, List<MonthPoint> savingsEvolution,
                            List<ActivityView> recentActivity, long unreadNotifications,
                            List<NotificationView> notifications, LearningSummary learning, QuizSummary quizzes,
                            GamificationSnapshot gamification, List<Recommendation> recommendations,
                            boolean hasTransactions) {
    }

    private final GoalService goals;
    private final TransactionService transactions;
    private final TransactionRepository transactionRepository;
    private final ReportService reports;
    private final ActivityLogRepository activity;
    private final NotificationService notifications;
    private final CourseService courses;
    private final QuizService quizzes;
    private final QuizAttemptRepository attempts;
    private final GamificationService gamification;
    private final AppTime time;

    public DashboardService(GoalService goals, TransactionService transactions,
                            TransactionRepository transactionRepository, ReportService reports,
                            ActivityLogRepository activity, NotificationService notifications, CourseService courses,
                            QuizService quizzes, QuizAttemptRepository attempts, GamificationService gamification,
                            AppTime time) {
        this.goals = goals;
        this.transactions = transactions;
        this.transactionRepository = transactionRepository;
        this.reports = reports;
        this.activity = activity;
        this.notifications = notifications;
        this.courses = courses;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.gamification = gamification;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public Dashboard build(Long userId, String displayName) {
        List<GoalView> allGoals = goals.list(userId, GoalService.Filter.ALL);
        List<GoalView> open = allGoals.stream().filter(g -> g.status() != Goal.Status.CANCELLED).toList();
        BigDecimal saved = open.stream().map(GoalView::savedAmount).reduce(Money.zero(), BigDecimal::add);
        BigDecimal target = open.stream().map(GoalView::targetAmount).reduce(Money.zero(), BigDecimal::add);
        List<GoalView> highlights = open.stream().filter(g -> g.status() == Goal.Status.ACTIVE)
                .sorted(Comparator.comparing(GoalView::progressPercent).reversed()).limit(3).toList();
        GoalsSummary goalsSummary = new GoalsSummary(
                (int) open.stream().filter(g -> g.status() == Goal.Status.ACTIVE).count(),
                (int) open.stream().filter(g -> g.status() == Goal.Status.COMPLETED).count(),
                saved, target, Money.progressPercent(saved, target), highlights);

        LocalDate today = time.today();
        Summary month = transactions.summary(userId, today.withDayOfMonth(1), today);
        List<MonthPoint> evolution = reports.finances(userId, today.minusMonths(5).withDayOfMonth(1), today).monthly();

        List<LearningItem> learning = courses.myLearning(userId);
        LearningSummary learningSummary = new LearningSummary(learning.size(),
                (int) learning.stream().filter(l -> l.completedAt() != null).count(),
                learning.stream().filter(l -> l.completedAt() == null).limit(3).toList());

        List<AttemptHistoryItem> recentQuizzes = quizzes.history(userId, 0, 3).content();
        List<QuizAttempt> completed = attempts.findByUserIdAndStatusOrderByFinishedAtAsc(userId, QuizAttempt.Status.COMPLETED);
        BigDecimal avg = completed.isEmpty() ? null : completed.stream().map(QuizAttempt::getPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(completed.size()), 1, RoundingMode.HALF_EVEN);

        GamificationService.Summary g = gamification.summary(userId);
        List<AchievementView> recentAchievements = g.achievements().stream().filter(AchievementView::unlocked)
                .sorted(Comparator.comparing(AchievementView::unlockedAt).reversed()).limit(3).toList();

        boolean hasTransactions = transactionRepository.countByUserId(userId) > 0;
        return new Dashboard(displayName, goalsSummary, month, evolution,
                activity.findTop8ByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(ActivityView::of).toList(),
                notifications.unreadCount(userId), notifications.latestUnread(userId), learningSummary,
                new QuizSummary(completed.size(), avg, recentQuizzes),
                new GamificationSnapshot(g.totalPoints(), g.level(), g.levelTitle(), g.currentLevelStart(),
                        g.nextLevelStart(), g.currentStreak(), g.achievementsUnlocked(), g.achievementsTotal(),
                        recentAchievements),
                recommendations(userId, allGoals, hasTransactions, learning, recentQuizzes),
                hasTransactions);
    }

    /** Recomendações educativas baseadas no uso real da plataforma (sem conselhos de investimento). */
    private List<Recommendation> recommendations(Long userId, List<GoalView> allGoals, boolean hasTransactions,
                                                 List<LearningItem> learning, List<AttemptHistoryItem> recentQuizzes) {
        List<Recommendation> list = new ArrayList<>();
        learning.stream().filter(l -> l.completedAt() == null && l.nextLessonId() != null).findFirst()
                .ifPresent(l -> list.add(new Recommendation("Continue \"" + l.title() + "\"",
                        "Você completou " + l.completedLessons() + " de " + l.lessonCount() + " aulas.",
                        "/aulas/" + l.nextLessonId(), "COURSE")));
        if (learning.isEmpty()) {
            list.add(new Recommendation("Comece pelo básico", "Faça o curso \"Primeiros passos com o dinheiro\".",
                    "/cursos", "COURSE"));
        }
        if (allGoals.isEmpty()) {
            list.add(new Recommendation("Crie sua primeira meta",
                    "Defina algo que você quer conquistar e acompanhe cada contribuição.", "/metas", "GOAL"));
        }
        if (!hasTransactions) {
            list.add(new Recommendation("Registre suas movimentações",
                    "Anotar o que entra e sai é o primeiro passo para organizar o orçamento.", "/transacoes", "HABIT"));
        }
        Set<Topic> weak = EnumSet.noneOf(Topic.class);
        recentQuizzes.stream().filter(q -> q.percent() != null && q.percent().compareTo(new BigDecimal("70")) < 0)
                .forEach(q -> weak.add(q.category()));
        for (CourseCard c : courses.recommendedFor(userId, weak, 2)) {
            list.add(new Recommendation("Reforce: " + c.categoryLabel(),
                    "Com base nos seus quizzes, o curso \"" + c.title() + "\" pode ajudar.", "/cursos/" + c.id(), "QUIZ"));
        }
        if (recentQuizzes.isEmpty()) {
            list.add(new Recommendation("Teste seus conhecimentos", "Faça um quiz e descubra o que revisar.",
                    "/quizzes", "QUIZ"));
        }
        return list.stream().limit(4).toList();
    }
}

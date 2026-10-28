package br.finedu.report;

import br.finedu.common.AppTime;
import br.finedu.common.Money;
import br.finedu.course.CourseDtos.LearningItem;
import br.finedu.course.CourseService;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.PointEventRepository;
import br.finedu.goal.Goal;
import br.finedu.goal.GoalDtos.GoalView;
import br.finedu.goal.GoalMovement;
import br.finedu.goal.GoalMovementRepository;
import br.finedu.goal.GoalService;
import br.finedu.quiz.QuizAttempt;
import br.finedu.quiz.QuizAttemptRepository;
import br.finedu.quiz.QuizRepository;
import br.finedu.transaction.Transaction;
import br.finedu.transaction.TransactionDtos.CategoryTotal;
import br.finedu.transaction.TransactionRepository;
import br.finedu.transaction.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios calculados sob demanda a partir dos registros originais (nenhum agregado é
 * persistido, portanto não há risco de divergência). Critérios:
 * <ul>
 *   <li>Transações: pela data informada pelo usuário (occurredOn), inclusive as simuladas.</li>
 *   <li>Saldo registrado: soma acumulada de receitas − despesas desde o primeiro registro.</li>
 *   <li>Metas: movimentações pela data/hora de registro, convertida para o fuso da aplicação.</li>
 *   <li>Quizzes: tentativas finalizadas no período; cursos: aulas/cursos concluídos no período.</li>
 * </ul>
 */
@Service
public class ReportService {

    public record MonthPoint(String month, BigDecimal income, BigDecimal expense, BigDecimal net,
                             BigDecimal cumulativeBalance) {
    }

    public record GoalMonthPoint(String month, BigDecimal contributions, BigDecimal withdrawals) {
    }

    public record FinancialReport(BigDecimal income, BigDecimal expense, BigDecimal balance, BigDecimal openingBalance,
                                  List<MonthPoint> monthly, List<CategoryTotal> expensesByCategory,
                                  List<CategoryTotal> incomeByCategory) {
    }

    public record GoalReport(int active, int completed, int cancelled, BigDecimal completionRate,
                             BigDecimal contributedInPeriod, BigDecimal withdrawnInPeriod, List<GoalMonthPoint> monthly,
                             List<GoalView> goals) {
    }

    public record QuizPoint(Instant finishedAt, String quizTitle, BigDecimal percent) {
    }

    public record QuizReport(int attempts, BigDecimal averagePercent, BigDecimal bestPercent, List<QuizPoint> evolution) {
    }

    public record CourseReport(int enrolled, int completedTotal, int completedInPeriod, List<LearningItem> courses) {
    }

    public record AchievementReport(long pointsInPeriod, long totalPoints, long unlocked, long total,
                                    List<GamificationService.AchievementView> unlockedInPeriod) {
    }

    public record FullReport(LocalDate from, LocalDate to, FinancialReport finances, GoalReport goals,
                             QuizReport quizzes, CourseReport courses, AchievementReport achievements,
                             List<String> criteria) {
    }

    private static final List<String> CRITERIA = List.of(
            "Receitas e despesas consideram a data informada em cada transação (inclusive registros simulados).",
            "O saldo registrado é a soma acumulada de receitas menos despesas desde o primeiro registro; não representa saldo bancário.",
            "Contribuições e retiradas de metas consideram a data em que foram registradas.",
            "Desempenho em quizzes considera tentativas finalizadas no período.",
            "Todos os valores são calculados no momento da consulta a partir dos registros originais.");

    private final TransactionRepository transactions;
    private final TransactionService transactionService;
    private final GoalService goalService;
    private final GoalMovementRepository goalMovements;
    private final QuizAttemptRepository attempts;
    private final QuizRepository quizzes;
    private final CourseService courses;
    private final GamificationService gamification;
    private final PointEventRepository points;
    private final AppTime time;

    public ReportService(TransactionRepository transactions, TransactionService transactionService,
                         GoalService goalService, GoalMovementRepository goalMovements, QuizAttemptRepository attempts,
                         QuizRepository quizzes, CourseService courses, GamificationService gamification,
                         PointEventRepository points, AppTime time) {
        this.transactions = transactions;
        this.transactionService = transactionService;
        this.goalService = goalService;
        this.goalMovements = goalMovements;
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.courses = courses;
        this.gamification = gamification;
        this.points = points;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public FullReport report(Long userId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : time.today();
        LocalDate start = from != null ? from : end.minusMonths(5).withDayOfMonth(1);
        TransactionService.validatePeriod(start, end);
        Instant startInstant = time.startOfDay(start);
        Instant endInstant = time.startOfDay(end.plusDays(1));
        return new FullReport(start, end, finances(userId, start, end), goals(userId, start, end, startInstant, endInstant),
                quizzes(userId, startInstant, endInstant), courses(userId, startInstant, endInstant),
                achievements(userId, startInstant, endInstant), CRITERIA);
    }

    public FinancialReport finances(Long userId, LocalDate start, LocalDate end) {
        BigDecimal opening = Money.of(transactions.balanceBefore(userId, start, Transaction.Type.INCOME));
        Map<String, BigDecimal[]> months = emptyMonths(start, end);
        BigDecimal income = Money.zero();
        BigDecimal expense = Money.zero();
        for (Object[] row : transactions.dailyTotals(userId, start, end)) {
            String key = YearMonth.from((LocalDate) row[0]).toString();
            BigDecimal value = (BigDecimal) row[2];
            BigDecimal[] slot = months.get(key);
            if (row[1] == Transaction.Type.INCOME) {
                slot[0] = slot[0].add(value);
                income = income.add(value);
            } else {
                slot[1] = slot[1].add(value);
                expense = expense.add(value);
            }
        }
        List<MonthPoint> monthly = new ArrayList<>();
        BigDecimal running = opening;
        for (Map.Entry<String, BigDecimal[]> e : months.entrySet()) {
            BigDecimal net = e.getValue()[0].subtract(e.getValue()[1]);
            running = running.add(net);
            monthly.add(new MonthPoint(e.getKey(), Money.of(e.getValue()[0]), Money.of(e.getValue()[1]), Money.of(net),
                    Money.of(running)));
        }
        return new FinancialReport(Money.of(income), Money.of(expense), Money.of(income.subtract(expense)), opening,
                monthly, transactionService.categoryTotals(userId, Transaction.Type.EXPENSE, start, end),
                transactionService.categoryTotals(userId, Transaction.Type.INCOME, start, end));
    }

    private GoalReport goals(Long userId, LocalDate start, LocalDate end, Instant from, Instant to) {
        List<GoalView> all = goalService.list(userId, GoalService.Filter.ALL);
        int active = (int) all.stream().filter(g -> g.status() == Goal.Status.ACTIVE).count();
        int completed = (int) all.stream().filter(g -> g.status() == Goal.Status.COMPLETED).count();
        int cancelled = (int) all.stream().filter(g -> g.status() == Goal.Status.CANCELLED).count();
        BigDecimal rate = active + completed == 0 ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_EVEN)
                : BigDecimal.valueOf(completed * 100L).divide(BigDecimal.valueOf(active + completed), 1, RoundingMode.HALF_EVEN);
        Map<String, BigDecimal[]> months = emptyMonths(start, end);
        BigDecimal contributed = Money.zero();
        BigDecimal withdrawn = Money.zero();
        for (GoalMovement m : goalMovements.findForUserBetween(userId, from, to)) {
            BigDecimal[] slot = months.get(YearMonth.from(time.toDate(m.getCreatedAt())).toString());
            if (m.getType() == GoalMovement.Type.CONTRIBUTION) {
                contributed = contributed.add(m.getAmount());
                if (slot != null) {
                    slot[0] = slot[0].add(m.getAmount());
                }
            } else {
                withdrawn = withdrawn.add(m.getAmount());
                if (slot != null) {
                    slot[1] = slot[1].add(m.getAmount());
                }
            }
        }
        List<GoalMonthPoint> monthly = months.entrySet().stream()
                .map(e -> new GoalMonthPoint(e.getKey(), Money.of(e.getValue()[0]), Money.of(e.getValue()[1]))).toList();
        return new GoalReport(active, completed, cancelled, rate, Money.of(contributed), Money.of(withdrawn), monthly, all);
    }

    private QuizReport quizzes(Long userId, Instant from, Instant to) {
        List<QuizAttempt> list = attempts.findCompletedBetween(userId, QuizAttempt.Status.COMPLETED, from, to);
        if (list.isEmpty()) {
            return new QuizReport(0, null, null, List.of());
        }
        BigDecimal sum = list.stream().map(QuizAttempt::getPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal best = list.stream().map(QuizAttempt::getPercent).max(BigDecimal::compareTo).orElse(null);
        Map<Long, String> titles = new LinkedHashMap<>();
        List<QuizPoint> evolution = list.stream().map(a -> new QuizPoint(a.getFinishedAt(),
                titles.computeIfAbsent(a.getQuizId(), id -> quizzes.findById(id).map(q -> q.getTitle()).orElse("Quiz")),
                a.getPercent())).toList();
        return new QuizReport(list.size(), sum.divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_EVEN), best,
                evolution);
    }

    private CourseReport courses(Long userId, Instant from, Instant to) {
        List<LearningItem> mine = courses.myLearning(userId);
        int completedTotal = (int) mine.stream().filter(c -> c.completedAt() != null).count();
        int completedInPeriod = (int) mine.stream().filter(c -> c.completedAt() != null
                && !c.completedAt().isBefore(from) && c.completedAt().isBefore(to)).count();
        return new CourseReport(mine.size(), completedTotal, completedInPeriod, mine);
    }

    private AchievementReport achievements(Long userId, Instant from, Instant to) {
        GamificationService.Summary s = gamification.summary(userId);
        List<GamificationService.AchievementView> inPeriod = s.achievements().stream()
                .filter(a -> a.unlocked() && !a.unlockedAt().isBefore(from) && a.unlockedAt().isBefore(to)).toList();
        long pointsInPeriod = points.pointsSince(userId, from) - points.pointsSince(userId, to);
        return new AchievementReport(pointsInPeriod, s.totalPoints(), s.achievementsUnlocked(), s.achievementsTotal(),
                inPeriod);
    }

    private static Map<String, BigDecimal[]> emptyMonths(LocalDate start, LocalDate end) {
        Map<String, BigDecimal[]> months = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
            months.put(m.toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        return months;
    }
}

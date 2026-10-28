package br.finedu.goal;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.Money;
import br.finedu.common.PageResponse;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.goal.GoalDtos.GoalRequest;
import br.finedu.goal.GoalDtos.GoalResult;
import br.finedu.goal.GoalDtos.GoalView;
import br.finedu.goal.GoalDtos.MovementRequest;
import br.finedu.goal.GoalDtos.MovementResult;
import br.finedu.goal.GoalDtos.MovementView;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    public enum Filter { ACTIVE, COMPLETED, CANCELLED, ALL }

    private final GoalRepository goals;
    private final GoalMovementRepository movements;
    private final GamificationService gamification;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final AppTime time;

    public GoalService(GoalRepository goals, GoalMovementRepository movements, GamificationService gamification,
                       NotificationService notifications, ActivityService activity, AppTime time) {
        this.goals = goals;
        this.movements = movements;
        this.gamification = gamification;
        this.notifications = notifications;
        this.activity = activity;
        this.time = time;
    }

    @Transactional
    public GoalResult create(Long userId, GoalRequest req) {
        validateDeadline(req.deadline(), null);
        Goal goal = goals.save(new Goal(userId, req.title().trim(), blankToNull(req.description()), req.category(),
                Money.requirePositive(req.targetAmount(), "O valor da meta"), req.deadline(), time.now()));
        activity.record(userId, ActivityLog.Type.GOAL_CREATED, "Meta criada: " + goal.getTitle());
        Reward reward = gamification.awardDaily(userId, PointRule.GOAL_CREATED);
        return new GoalResult(view(goal, Money.zero()), reward);
    }

    @Transactional
    public GoalView update(Long userId, Long goalId, GoalRequest req) {
        Goal goal = goals.lockByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
        validateDeadline(req.deadline(), goal.getDeadline());
        goal.update(req.title().trim(), blankToNull(req.description()), req.category(),
                Money.requirePositive(req.targetAmount(), "O valor da meta"), req.deadline(), time.now());
        BigDecimal saved = movements.balance(goalId);
        boolean completed = goal.refreshStatus(saved, time.now());
        if (completed) {
            onCompleted(userId, goal);
        }
        activity.record(userId, ActivityLog.Type.GOAL_UPDATED, "Meta atualizada: " + goal.getTitle());
        return view(goal, saved);
    }

    @Transactional
    public GoalView cancel(Long userId, Long goalId) {
        Goal goal = goals.lockByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
        goal.cancel(time.now());
        activity.record(userId, ActivityLog.Type.GOAL_CANCELLED, "Meta cancelada: " + goal.getTitle());
        return view(goal, movements.balance(goalId));
    }

    /**
     * Registra contribuição ou retirada. A meta é bloqueada (SELECT ... FOR UPDATE) durante a
     * operação, garantindo que retiradas concorrentes não deixem o saldo negativo.
     */
    @Transactional
    public MovementResult addMovement(Long userId, Long goalId, MovementRequest req) {
        Goal goal = goals.lockByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
        goal.requireNotCancelled();
        BigDecimal amount = Money.requirePositive(req.amount(), "O valor");
        BigDecimal saved = movements.balance(goalId);
        if (req.type() == GoalMovement.Type.WITHDRAWAL && amount.compareTo(saved) > 0) {
            throw ApiException.business("INSUFFICIENT_BALANCE",
                    "Não é possível retirar mais do que o valor acumulado na meta (R$ " + Money.of(saved) + ").");
        }
        GoalMovement movement = movements.save(new GoalMovement(goalId, req.type(), amount,
                blankToNull(req.note()), time.now()));
        BigDecimal newSaved = req.type() == GoalMovement.Type.CONTRIBUTION ? saved.add(amount) : saved.subtract(amount);
        boolean completed = goal.refreshStatus(newSaved, time.now());
        activity.record(userId, ActivityLog.Type.GOAL_MOVEMENT,
                (req.type() == GoalMovement.Type.CONTRIBUTION ? "Contribuição" : "Retirada") + " na meta " + goal.getTitle());
        Reward reward = completed ? onCompleted(userId, goal) : Reward.none();
        return new MovementResult(view(goal, newSaved), MovementView.of(movement), completed, reward);
    }

    private Reward onCompleted(Long userId, Goal goal) {
        activity.record(userId, ActivityLog.Type.GOAL_COMPLETED, "Meta concluída: " + goal.getTitle());
        notifications.notify(userId, NotificationType.GOAL_COMPLETED, "Meta alcançada! 🎉",
                "Parabéns! Você atingiu o valor da meta \"" + goal.getTitle() + "\".", "/metas/" + goal.getId(),
                "GOAL_COMPLETED:" + goal.getId());
        return gamification.awardDaily(userId, PointRule.GOAL_COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<GoalView> list(Long userId, Filter filter) {
        List<Goal> list = switch (filter) {
            case ALL -> goals.findByUserIdOrderByCreatedAtDesc(userId);
            case ACTIVE -> goals.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, EnumSet.of(Goal.Status.ACTIVE));
            case COMPLETED -> goals.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, EnumSet.of(Goal.Status.COMPLETED));
            case CANCELLED -> goals.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, EnumSet.of(Goal.Status.CANCELLED));
        };
        Map<Long, BigDecimal> balances = balances(list);
        return list.stream().map(g -> view(g, balances.getOrDefault(g.getId(), Money.zero()))).toList();
    }

    @Transactional(readOnly = true)
    public GoalView get(Long userId, Long goalId) {
        Goal goal = goals.findByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
        return view(goal, movements.balance(goalId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementView> movements(Long userId, Long goalId, int page, int size) {
        goals.findByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
        return PageResponse.of(movements.findByGoalIdOrderByCreatedAtDescIdDesc(goalId, PageRequest.of(page, size)),
                MovementView::of);
    }

    /** Confirma que a meta pertence ao usuário (usado por outros módulos, ex.: transações). */
    @Transactional(readOnly = true)
    public void requireOwned(Long userId, Long goalId) {
        goals.findByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Meta"));
    }

    public Map<Long, BigDecimal> balances(List<Goal> list) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (list.isEmpty()) {
            return result;
        }
        for (Object[] row : movements.balances(list.stream().map(Goal::getId).toList(),
                GoalMovement.Type.CONTRIBUTION)) {
            result.put((Long) row[0], Money.of((BigDecimal) row[1]));
        }
        return result;
    }

    public GoalView view(Goal g, BigDecimal saved) {
        BigDecimal savedAmount = Money.of(saved);
        BigDecimal remaining = g.getTargetAmount().subtract(savedAmount).max(BigDecimal.ZERO);
        LocalDate today = time.today();
        Long daysRemaining = g.getDeadline() == null ? null : ChronoUnit.DAYS.between(today, g.getDeadline());
        BigDecimal monthly = null;
        if (g.getStatus() == Goal.Status.ACTIVE && g.getDeadline() != null && remaining.signum() > 0) {
            long months = Math.max(1, ChronoUnit.MONTHS.between(today.withDayOfMonth(1), g.getDeadline().withDayOfMonth(1)));
            monthly = remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING);
        }
        return new GoalView(g.getId(), g.getTitle(), g.getDescription(), g.getCategory(), Money.of(g.getTargetAmount()),
                savedAmount, Money.of(remaining), Money.progressPercent(savedAmount, g.getTargetAmount()),
                g.getDeadline(), daysRemaining, monthly, g.getStatus(), g.getCreatedAt(), g.getCompletedAt());
    }

    private void validateDeadline(LocalDate deadline, LocalDate current) {
        if (deadline != null && deadline.isBefore(time.today()) && !deadline.equals(current)) {
            throw ApiException.business("INVALID_DEADLINE", "O prazo não pode estar no passado.");
        }
        if (deadline != null && deadline.isAfter(time.today().plusYears(30))) {
            throw ApiException.business("INVALID_DEADLINE", "O prazo deve ser de no máximo 30 anos.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

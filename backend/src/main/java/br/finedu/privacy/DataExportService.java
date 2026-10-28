package br.finedu.privacy;

import br.finedu.calculator.SimulationRepository;
import br.finedu.course.CourseService;
import br.finedu.feedback.FeedbackRepository;
import br.finedu.forum.ForumCommentRepository;
import br.finedu.forum.ForumPostRepository;
import br.finedu.gamification.PointEventRepository;
import br.finedu.goal.GoalMovementRepository;
import br.finedu.goal.GoalService;
import br.finedu.notification.NotificationRepository;
import br.finedu.portfolio.WalletMovementRepository;
import br.finedu.portfolio.WalletRepository;
import br.finedu.quiz.QuizAttempt;
import br.finedu.quiz.QuizAttemptRepository;
import br.finedu.transaction.TransactionDtos.TransactionView;
import br.finedu.transaction.TransactionRepository;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityLogRepository;
import br.finedu.user.ActivityService;
import br.finedu.user.UserService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Portabilidade/acesso aos dados (LGPD art. 18): reúne, em JSON, todos os dados pessoais do
 * próprio usuário. Não inclui hash de senha, tokens nem dados de outras pessoas.
 */
@Service
public class DataExportService {

    private final UserService users;
    private final GoalService goals;
    private final GoalMovementRepository goalMovements;
    private final TransactionRepository transactions;
    private final SimulationRepository simulations;
    private final QuizAttemptRepository attempts;
    private final CourseService courses;
    private final PointEventRepository points;
    private final NotificationRepository notifications;
    private final ForumPostRepository posts;
    private final ForumCommentRepository comments;
    private final WalletRepository wallets;
    private final WalletMovementRepository walletMovements;
    private final FeedbackRepository feedback;
    private final ActivityLogRepository activityLogs;
    private final ActivityService activity;

    public DataExportService(UserService users, GoalService goals, GoalMovementRepository goalMovements,
                             TransactionRepository transactions, SimulationRepository simulations,
                             QuizAttemptRepository attempts, CourseService courses, PointEventRepository points,
                             NotificationRepository notifications, ForumPostRepository posts,
                             ForumCommentRepository comments, WalletRepository wallets,
                             WalletMovementRepository walletMovements, FeedbackRepository feedback,
                             ActivityLogRepository activityLogs, ActivityService activity) {
        this.users = users;
        this.goals = goals;
        this.goalMovements = goalMovements;
        this.transactions = transactions;
        this.simulations = simulations;
        this.attempts = attempts;
        this.courses = courses;
        this.points = points;
        this.notifications = notifications;
        this.posts = posts;
        this.comments = comments;
        this.wallets = wallets;
        this.walletMovements = walletMovements;
        this.feedback = feedback;
        this.activityLogs = activityLogs;
        this.activity = activity;
    }

    @Transactional
    public Map<String, Object> export(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", Instant.now());
        data.put("profile", users.me(userId));
        data.put("goals", goals.list(userId, GoalService.Filter.ALL));
        data.put("goalMovements", goalMovements.findAllForUser(userId).stream().map(m -> Map.of(
                "goalId", m.getGoalId(), "type", m.getType(), "amount", m.getAmount(), "createdAt", m.getCreatedAt()))
                .toList());
        data.put("transactions", transactions.findByUserIdOrderByOccurredOnAscIdAsc(userId).stream()
                .map(TransactionView::of).toList());
        data.put("simulations", simulations.findByUserIdOrderByCreatedAtDesc(userId).stream().map(s -> Map.of(
                "name", s.getName(), "input", s.toInput(), "finalAmount", s.getFinalAmount(),
                "createdAt", s.getCreatedAt())).toList());
        data.put("quizAttempts", attempts.findByUserIdAndStatusOrderByFinishedAtAsc(userId, QuizAttempt.Status.COMPLETED)
                .stream().map(a -> Map.of("quizId", a.getQuizId(), "correct", a.getCorrectCount(),
                        "total", a.getTotalQuestions(), "percent", a.getPercent(), "finishedAt", a.getFinishedAt()))
                .toList());
        data.put("courses", courses.myLearning(userId));
        data.put("lessonNotes", courses.myNotes(userId));
        data.put("points", points.findByUserIdOrderByCreatedAtAsc(userId).stream().map(p -> Map.of(
                "type", p.getEventType(), "points", p.getPoints(), "createdAt", p.getCreatedAt())).toList());
        data.put("notifications", notifications.findByUserIdOrderByCreatedAtAsc(userId).stream().map(n -> Map.of(
                "title", n.getTitle(), "message", n.getMessage(), "createdAt", n.getCreatedAt())).toList());
        data.put("forumPosts", posts.findByAuthorIdOrderByCreatedAtAsc(userId).stream().map(p -> Map.of(
                "title", p.getTitle(), "body", p.getBody(), "status", p.getStatus(), "createdAt", p.getCreatedAt()))
                .toList());
        data.put("forumComments", comments.findByAuthorIdOrderByCreatedAtAsc(userId).stream().map(c -> Map.of(
                "postId", c.getPostId(), "body", c.getBody(), "createdAt", c.getCreatedAt())).toList());
        data.put("simulatedWallets", wallets.findByUserIdOrderByCreatedAtDesc(userId).stream().map(w -> Map.of(
                "name", w.getName(), "assumedAnnualRate", w.getAssumedAnnualRate(),
                "movements", walletMovements.findByWalletIdOrderByOccurredOnAscIdAsc(w.getId()).stream()
                        .map(m -> Map.of("type", m.getType(), "amount", m.getAmount(), "date", m.getOccurredOn()))
                        .toList())).toList());
        data.put("feedback", feedback.findByUserIdOrderByCreatedAtDesc(userId).stream().map(f -> Map.of(
                "subject", f.getSubject(), "message", f.getMessage(), "status", f.getStatus())).toList());
        data.put("activity", activityLogs.findByUserIdOrderByCreatedAtAsc(userId).stream().map(a -> Map.of(
                "type", a.getType(), "description", a.getDescription(), "createdAt", a.getCreatedAt())).toList());
        activity.record(userId, ActivityLog.Type.DATA_EXPORTED, "Exportação dos dados pessoais");
        return data;
    }
}

package br.finedu.notification;

import br.finedu.common.AppTime;
import br.finedu.course.EnrollmentRepository;
import br.finedu.gamification.PointEventRepository;
import br.finedu.gamification.PointRule;
import br.finedu.goal.Goal;
import br.finedu.goal.GoalRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lembretes diários (notificações internas). Cada lembrete tem chave de deduplicação, então
 * reexecutar o job não gera notificações repetidas.
 */
@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    static final int DEADLINE_WINDOW_DAYS = 7;
    static final int INACTIVITY_DAYS = 3;

    private final GoalRepository goals;
    private final EnrollmentRepository enrollments;
    private final PointEventRepository points;
    private final NotificationService notifications;
    private final AppTime time;

    public ReminderJob(GoalRepository goals, EnrollmentRepository enrollments, PointEventRepository points,
                       NotificationService notifications, AppTime time) {
        this.goals = goals;
        this.enrollments = enrollments;
        this.points = points;
        this.notifications = notifications;
        this.time = time;
    }

    @Scheduled(cron = "${app.scheduler.reminders-cron}", zone = "${app.zone:America/Sao_Paulo}")
    @Transactional
    public int run() {
        LocalDate today = time.today();
        int created = 0;
        for (Goal g : goals.findByStatusAndDeadlineIsNotNullAndDeadlineLessThanEqual(Goal.Status.ACTIVE,
                today.plusDays(DEADLINE_WINDOW_DAYS))) {
            long days = ChronoUnit.DAYS.between(today, g.getDeadline());
            boolean ok = days < 0
                    ? notifications.notify(g.getUserId(), NotificationType.GOAL_OVERDUE, "Prazo da meta encerrado",
                    "O prazo da meta \"" + g.getTitle() + "\" passou. Que tal revisar o prazo ou o valor?",
                    "/metas/" + g.getId(), "GOAL_OVERDUE:" + g.getId() + ":" + g.getDeadline())
                    : notifications.notify(g.getUserId(), NotificationType.GOAL_DEADLINE, "Prazo da meta se aproximando",
                    "Faltam " + days + " dia(s) para o prazo da meta \"" + g.getTitle() + "\".",
                    "/metas/" + g.getId(), "GOAL_DEADLINE:" + g.getId() + ":" + g.getDeadline());
            created += ok ? 1 : 0;
        }
        var since = time.startOfDay(today.minusDays(INACTIVITY_DAYS));
        for (Long userId : enrollments.usersWithCoursesInProgress()) {
            if (points.countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(userId, PointRule.LESSON_COMPLETED, since) == 0) {
                boolean ok = notifications.notify(userId, NotificationType.LEARNING_REMINDER, "Bora continuar aprendendo?",
                        "Você tem um curso em andamento. Que tal fazer uma aula hoje?", "/cursos",
                        "LEARNING_REMINDER:" + time.isoWeekKey());
                created += ok ? 1 : 0;
            }
        }
        log.info("Lembretes gerados: {}", created);
        return created;
    }
}

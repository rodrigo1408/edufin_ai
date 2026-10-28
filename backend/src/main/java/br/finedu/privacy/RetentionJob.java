package br.finedu.privacy;

import br.finedu.common.AppTime;
import br.finedu.notification.NotificationRepository;
import br.finedu.user.ActivityLogRepository;
import br.finedu.user.PasswordResetTokenRepository;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Política de retenção (minimização de dados): remove históricos de atividade antigos,
 * notificações lidas antigas e tokens de redefinição expirados. Prazos configuráveis.
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final ActivityLogRepository activity;
    private final NotificationRepository notifications;
    private final PasswordResetTokenRepository tokens;
    private final AppTime time;
    private final int activityDays;
    private final int notificationDays;

    public RetentionJob(ActivityLogRepository activity, NotificationRepository notifications,
                        PasswordResetTokenRepository tokens, AppTime time,
                        @Value("${app.retention.activity-log-days}") int activityDays,
                        @Value("${app.retention.read-notification-days}") int notificationDays) {
        this.activity = activity;
        this.notifications = notifications;
        this.tokens = tokens;
        this.time = time;
        this.activityDays = activityDays;
        this.notificationDays = notificationDays;
    }

    @Scheduled(cron = "${app.scheduler.retention-cron}", zone = "${app.zone:America/Sao_Paulo}")
    @Transactional
    public void run() {
        int a = activity.deleteOlderThan(time.now().minus(Duration.ofDays(activityDays)));
        int n = notifications.deleteReadBefore(time.now().minus(Duration.ofDays(notificationDays)));
        int t = tokens.deleteExpiredBefore(time.now().minus(Duration.ofDays(1)));
        log.info("Retenção: {} atividades, {} notificações e {} tokens removidos", a, n, t);
    }
}

package br.finedu.notification;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.PageResponse;
import br.finedu.user.UserPreferences;
import br.finedu.user.UserPreferencesRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notificações internas (dentro da plataforma). Não há envio por e-mail/push: esses canais
 * dependem de infraestrutura externa ainda não configurada e não são simulados.
 */
@Service
public class NotificationService {

    public record NotificationView(Long id, NotificationType type, String title, String message, String link,
                                   boolean read, Instant createdAt) {
        static NotificationView of(Notification n) {
            return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(),
                    n.getReadAt() != null, n.getCreatedAt());
        }
    }

    private final NotificationRepository repository;
    private final UserPreferencesRepository preferences;
    private final AppTime time;

    public NotificationService(NotificationRepository repository, UserPreferencesRepository preferences,
                               AppTime time) {
        this.repository = repository;
        this.preferences = preferences;
        this.time = time;
    }

    /**
     * Cria uma notificação respeitando as preferências do usuário. Com {@code dedupKey}, garante
     * no máximo uma notificação por chave (lembretes agendados não se repetem).
     *
     * @return true se a notificação foi criada
     */
    @Transactional
    public boolean notify(Long userId, NotificationType type, String title, String message, String link,
                          String dedupKey) {
        if (!enabledFor(userId, type)) {
            return false;
        }
        if (dedupKey != null && repository.existsByUserIdAndDedupKey(userId, dedupKey)) {
            return false;
        }
        repository.save(new Notification(userId, type, title, message, link, dedupKey, time.now()));
        return true;
    }

    private boolean enabledFor(Long userId, NotificationType type) {
        UserPreferences p = preferences.getOrDefault(userId);
        return switch (type.category()) {
            case SYSTEM -> true;
            case GOALS -> p.isNotifyGoals();
            case LEARNING -> p.isNotifyLearning();
            case ACHIEVEMENTS -> p.isNotifyAchievements();
            case COMMUNITY -> p.isNotifyCommunity();
        };
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationView> list(Long userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId, pageable)
                : repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        return PageResponse.of(page, NotificationView::of);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> latestUnread(Long userId) {
        return repository.findTop5ByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationView::of).toList();
    }

    @Transactional
    public NotificationView markRead(Long userId, Long id) {
        Notification n = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notificação"));
        n.markRead(time.now());
        return NotificationView.of(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId, time.now());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Notification n = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Notificação"));
        repository.delete(n);
    }
}

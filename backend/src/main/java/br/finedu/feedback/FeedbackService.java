package br.finedu.feedback;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.ContentPolicy;
import br.finedu.common.PageResponse;
import br.finedu.common.RateLimiter;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import br.finedu.user.User;
import br.finedu.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    public record FeedbackRequest(@NotNull Feedback.Type type, @NotBlank @Size(max = 120) String subject,
                                  @NotBlank @Size(min = 5, max = 2000) String message) {
    }

    public record HandleRequest(@NotNull Feedback.Status status, @Size(max = 1000) String response) {
    }

    public record FeedbackView(Long id, Feedback.Type type, String subject, String message, Feedback.Status status,
                               String adminResponse, String author, Instant createdAt, Instant updatedAt) {
    }

    private final FeedbackRepository repository;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final RateLimiter rateLimiter;
    private final AppTime time;

    public FeedbackService(FeedbackRepository repository, UserRepository users, NotificationService notifications,
                           ActivityService activity, RateLimiter rateLimiter, AppTime time) {
        this.repository = repository;
        this.users = users;
        this.notifications = notifications;
        this.activity = activity;
        this.rateLimiter = rateLimiter;
        this.time = time;
    }

    @Transactional
    public FeedbackView send(Long userId, FeedbackRequest req) {
        rateLimiter.check("feedback:" + userId, 10, Duration.ofHours(1));
        Feedback f = repository.save(new Feedback(userId, req.type(), ContentPolicy.sanitize(req.subject()),
                ContentPolicy.sanitize(req.message()), time.now()));
        activity.record(userId, ActivityLog.Type.FEEDBACK_SENT, "Feedback enviado: " + f.getSubject());
        return view(f, null);
    }

    @Transactional(readOnly = true)
    public List<FeedbackView> mine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(f -> view(f, null)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackView> search(Feedback.Status status, Feedback.Type type, int page, int size) {
        Page<Feedback> p = repository.search(status, type, PageRequest.of(page, size));
        Map<Long, String> names = users.findAllById(p.getContent().stream().map(Feedback::getUserId)
                        .filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(User::getId, User::getDisplayName));
        return PageResponse.of(p, f -> view(f, f.getUserId() == null ? "Conta excluída" : names.get(f.getUserId())));
    }

    @Transactional
    public FeedbackView handle(Long adminId, Long id, HandleRequest req) {
        Feedback f = repository.findById(id).orElseThrow(() -> ApiException.notFound("Feedback"));
        String response = req.response() == null || req.response().isBlank() ? f.getAdminResponse()
                : ContentPolicy.sanitize(req.response());
        boolean newResponse = response != null && !response.equals(f.getAdminResponse());
        f.handle(req.status(), response, adminId, time.now());
        if (newResponse && f.getUserId() != null) {
            notifications.notify(f.getUserId(), NotificationType.FEEDBACK_ANSWERED, "Seu feedback foi respondido",
                    "A equipe respondeu: \"" + f.getSubject() + "\".", "/feedback", null);
        }
        return view(f, null);
    }

    private static FeedbackView view(Feedback f, String author) {
        return new FeedbackView(f.getId(), f.getType(), f.getSubject(), f.getMessage(), f.getStatus(),
                f.getAdminResponse(), author, f.getCreatedAt(), f.getUpdatedAt());
    }
}

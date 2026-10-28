package br.finedu.forum;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.PageResponse;
import br.finedu.course.CourseReview;
import br.finedu.course.CourseReviewRepository;
import br.finedu.forum.ForumDtos.ModeratedItem;
import br.finedu.forum.ForumDtos.ModerationAction;
import br.finedu.forum.ForumDtos.ReportView;
import br.finedu.forum.ForumEntities.ReportStatus;
import br.finedu.forum.ForumEntities.Status;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ações exclusivas de moderadores e administradores (permissão FORUM_MODERATE). */
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    public record ReviewModerationView(Long id, Long courseId, String author, int rating, String comment,
                                       boolean hidden, Instant createdAt) {
    }

    private final ForumPostRepository posts;
    private final ForumCommentRepository comments;
    private final ForumReportRepository reports;
    private final CourseReviewRepository reviews;
    private final ForumService forum;
    private final NotificationService notifications;
    private final AppTime time;

    public ModerationService(ForumPostRepository posts, ForumCommentRepository comments, ForumReportRepository reports,
                             CourseReviewRepository reviews, ForumService forum, NotificationService notifications,
                             AppTime time) {
        this.posts = posts;
        this.comments = comments;
        this.reports = reports;
        this.reviews = reviews;
        this.forum = forum;
        this.notifications = notifications;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportView> reports(ReportStatus status, int page, int size) {
        Page<ForumReport> p = reports.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size));
        return PageResponse.of(p, this::reportView);
    }

    /** Aplica a ação ao conteúdo denunciado e encerra todas as denúncias abertas sobre ele. */
    @Transactional
    public ReportView resolve(Long moderatorId, Long reportId, ModerationAction action) {
        ForumReport report = reports.findById(reportId).orElseThrow(() -> ApiException.notFound("Denúncia"));
        List<ForumReport> related;
        if (report.getPostId() != null) {
            ForumPost post = posts.findById(report.getPostId()).orElseThrow(() -> ApiException.notFound("Publicação"));
            applyToPost(post, action);
            related = reports.findByPostIdAndStatus(post.getId(), ReportStatus.OPEN);
        } else {
            ForumComment c = comments.findById(report.getCommentId()).orElseThrow(() -> ApiException.notFound("Comentário"));
            applyToComment(c, action);
            related = reports.findByCommentIdAndStatus(c.getId(), ReportStatus.OPEN);
        }
        ReportStatus closing = action == ModerationAction.DISMISS || action == ModerationAction.RESTORE
                ? ReportStatus.DISMISSED : ReportStatus.RESOLVED;
        related.forEach(r -> r.close(closing, moderatorId, time.now()));
        if (report.getStatus() == ReportStatus.OPEN) {
            report.close(closing, moderatorId, time.now());
        }
        log.info("Moderação: denúncia={} ação={} moderador={}", reportId, action, moderatorId);
        return reportView(report);
    }

    @Transactional
    public void moderatePost(Long moderatorId, Long postId, ModerationAction action) {
        ForumPost post = posts.findById(postId).orElseThrow(() -> ApiException.notFound("Publicação"));
        applyToPost(post, action);
        log.info("Moderação: post={} ação={} moderador={}", postId, action, moderatorId);
    }

    @Transactional
    public void moderateComment(Long moderatorId, Long commentId, ModerationAction action) {
        ForumComment c = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comentário"));
        applyToComment(c, action);
        log.info("Moderação: comentário={} ação={} moderador={}", commentId, action, moderatorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModeratedItem> moderated(Status status, int page, int size) {
        Page<ForumPost> p = posts.findByStatusOrderByUpdatedAtDesc(status, PageRequest.of(page, size));
        Map<Long, String> names = forum.names(p.getContent().stream().map(ForumPost::getAuthorId).toList());
        return PageResponse.of(p, x -> new ModeratedItem(x.getId(), "POST", x.getId(), x.getTitle(), excerpt(x.getBody()),
                names.getOrDefault(x.getAuthorId(), "Usuário"), x.getStatus(), x.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ModeratedItem> moderatedComments(Status status, int page, int size) {
        Page<ForumComment> p = comments.findByStatusOrderByUpdatedAtDesc(status, PageRequest.of(page, size));
        Map<Long, String> names = forum.names(p.getContent().stream().map(ForumComment::getAuthorId).toList());
        return PageResponse.of(p, x -> new ModeratedItem(x.getId(), "COMMENT", x.getPostId(), null, excerpt(x.getBody()),
                names.getOrDefault(x.getAuthorId(), "Usuário"), x.getStatus(), x.getUpdatedAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewModerationView> courseReviews(int page, int size) {
        Page<CourseReview> p = reviews.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<Long, String> names = forum.names(p.getContent().stream().map(CourseReview::getUserId).toList());
        return PageResponse.of(p, r -> new ReviewModerationView(r.getId(), r.getCourseId(),
                names.getOrDefault(r.getUserId(), "Usuário"), r.getRating(), r.getComment(), r.isHidden(),
                r.getCreatedAt()));
    }

    @Transactional
    public void setReviewHidden(Long reviewId, boolean hidden) {
        reviews.findById(reviewId).orElseThrow(() -> ApiException.notFound("Avaliação")).setHidden(hidden);
    }

    private void applyToPost(ForumPost post, ModerationAction action) {
        Status target = targetStatus(post.getStatus(), action);
        if (target != post.getStatus()) {
            post.setStatus(target, time.now());
            if (target != Status.VISIBLE) {
                notifications.notify(post.getAuthorId(), NotificationType.CONTENT_MODERATED, "Publicação moderada",
                        "Sua publicação \"" + post.getTitle() + "\" foi " + (target == Status.HIDDEN ? "ocultada" : "removida")
                                + " por não seguir as regras da comunidade.", "/comunidade", null);
            }
        }
    }

    private void applyToComment(ForumComment c, ModerationAction action) {
        Status target = targetStatus(c.getStatus(), action);
        if (target != c.getStatus()) {
            c.setStatus(target, time.now());
            if (target != Status.VISIBLE) {
                notifications.notify(c.getAuthorId(), NotificationType.CONTENT_MODERATED, "Comentário moderado",
                        "Um comentário seu foi " + (target == Status.HIDDEN ? "ocultado" : "removido")
                                + " por não seguir as regras da comunidade.", "/comunidade/" + c.getPostId(), null);
            }
        }
    }

    private static Status targetStatus(Status current, ModerationAction action) {
        return switch (action) {
            case HIDE -> Status.HIDDEN;
            case REMOVE -> Status.REMOVED;
            case RESTORE -> Status.VISIBLE;
            case DISMISS -> current == Status.HIDDEN ? Status.VISIBLE : current;
        };
    }

    private ReportView reportView(ForumReport r) {
        if (r.getPostId() != null) {
            ForumPost p = posts.findById(r.getPostId()).orElse(null);
            return new ReportView(r.getId(), "POST", r.getPostId(), r.getPostId(), p == null ? null : p.getTitle(),
                    p == null ? null : excerpt(p.getBody()),
                    p == null ? null : forum.names(List.of(p.getAuthorId())).get(p.getAuthorId()),
                    p == null ? null : p.getStatus(), r.getReason(), r.getDetails(), r.getStatus(),
                    reports.countByPostIdAndStatus(r.getPostId(), ReportStatus.OPEN), r.getCreatedAt());
        }
        ForumComment c = comments.findById(r.getCommentId()).orElse(null);
        return new ReportView(r.getId(), "COMMENT", r.getCommentId(), c == null ? null : c.getPostId(), null,
                c == null ? null : excerpt(c.getBody()),
                c == null ? null : forum.names(List.of(c.getAuthorId())).get(c.getAuthorId()),
                c == null ? null : c.getStatus(), r.getReason(), r.getDetails(), r.getStatus(),
                reports.countByCommentIdAndStatus(r.getCommentId(), ReportStatus.OPEN), r.getCreatedAt());
    }

    private static String excerpt(String body) {
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}

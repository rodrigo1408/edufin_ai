package br.finedu.forum;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.ContentPolicy;
import br.finedu.common.PageResponse;
import br.finedu.common.RateLimiter;
import br.finedu.forum.ForumDtos.CommentRequest;
import br.finedu.forum.ForumDtos.CommentView;
import br.finedu.forum.ForumDtos.LikeResult;
import br.finedu.forum.ForumDtos.PostDetail;
import br.finedu.forum.ForumDtos.PostRequest;
import br.finedu.forum.ForumDtos.PostSummary;
import br.finedu.forum.ForumDtos.ReportRequest;
import br.finedu.forum.ForumEntities.Category;
import br.finedu.forum.ForumEntities.ReportStatus;
import br.finedu.forum.ForumEntities.Status;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.transaction.TransactionService;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import br.finedu.user.User;
import br.finedu.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comunidade educativa. Regras: apenas o autor edita/exclui o próprio conteúdo; textos passam pelo
 * {@link ContentPolicy}; há limite de frequência para publicar; conteúdo com 3 denúncias abertas é
 * ocultado automaticamente até revisão da moderação. Somente o apelido do autor é exibido.
 */
@Service
public class ForumService {

    public static final int AUTO_HIDE_THRESHOLD = 3;

    private final ForumPostRepository posts;
    private final ForumCommentRepository comments;
    private final ForumLikeRepository likes;
    private final ForumReportRepository reports;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final RateLimiter rateLimiter;
    private final AppTime time;

    public ForumService(ForumPostRepository posts, ForumCommentRepository comments, ForumLikeRepository likes,
                        ForumReportRepository reports, UserRepository users, NotificationService notifications,
                        ActivityService activity, RateLimiter rateLimiter, AppTime time) {
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.reports = reports;
        this.users = users;
        this.notifications = notifications;
        this.activity = activity;
        this.rateLimiter = rateLimiter;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummary> list(Long userId, String q, Category category, boolean mine, int page, int size) {
        Specification<ForumPost> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), Status.VISIBLE));
            if (category != null) {
                p.add(cb.equal(root.get("category"), category));
            }
            if (mine) {
                p.add(cb.equal(root.get("authorId"), userId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + TransactionService.escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("body")), like, '\\')));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Page<ForumPost> result = posts.findAll(spec, PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return PageResponse.of(result, summaries(userId, result.getContent())::get);
    }

    @Transactional(readOnly = true)
    public PostDetail get(Long userId, Long postId) {
        ForumPost post = visiblePost(postId);
        return new PostDetail(summaries(userId, List.of(post)).get(post), post.getBody());
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentView> comments(Long userId, Long postId, int page, int size) {
        visiblePost(postId);
        Page<ForumComment> p = comments.findByPostIdAndStatusOrderByCreatedAtAsc(postId, Status.VISIBLE,
                PageRequest.of(page, size));
        Map<Long, String> names = names(p.getContent().stream().map(ForumComment::getAuthorId).toList());
        return PageResponse.of(p, c -> new CommentView(c.getId(), names.getOrDefault(c.getAuthorId(), "Usuário"),
                c.getBody(), c.getCreatedAt(), !c.getUpdatedAt().equals(c.getCreatedAt()),
                c.getAuthorId().equals(userId)));
    }

    @Transactional
    public PostDetail create(Long userId, PostRequest req) {
        rateLimiter.check("forum-post:" + userId, 5, Duration.ofMinutes(10));
        ForumPost post = posts.save(new ForumPost(userId, req.category(), ContentPolicy.check(req.title()),
                ContentPolicy.check(req.body()), time.now()));
        activity.record(userId, ActivityLog.Type.POST_CREATED, "Publicação criada: " + post.getTitle());
        return get(userId, post.getId());
    }

    @Transactional
    public PostDetail update(Long userId, Long postId, PostRequest req) {
        ForumPost post = ownPost(userId, postId);
        if (post.getStatus() != Status.VISIBLE) {
            throw ApiException.business("POST_MODERATED", "Esta publicação está em moderação e não pode ser editada.");
        }
        post.edit(req.category(), ContentPolicy.check(req.title()), ContentPolicy.check(req.body()), time.now());
        return get(userId, postId);
    }

    @Transactional
    public void delete(Long userId, Long postId) {
        posts.delete(ownPost(userId, postId));
    }

    @Transactional
    public CommentView comment(Long userId, Long postId, CommentRequest req) {
        rateLimiter.check("forum-comment:" + userId, 15, Duration.ofMinutes(10));
        ForumPost post = visiblePost(postId);
        ForumComment c = comments.save(new ForumComment(postId, userId, ContentPolicy.check(req.body()), time.now()));
        if (!post.getAuthorId().equals(userId)) {
            notifications.notify(post.getAuthorId(), NotificationType.POST_COMMENTED, "Novo comentário",
                    "Alguém comentou na sua publicação \"" + post.getTitle() + "\".", "/comunidade/" + postId, null);
        }
        return new CommentView(c.getId(), names(List.of(userId)).get(userId), c.getBody(), c.getCreatedAt(), false, true);
    }

    @Transactional
    public CommentView editComment(Long userId, Long commentId, CommentRequest req) {
        ForumComment c = ownComment(userId, commentId);
        if (c.getStatus() != Status.VISIBLE) {
            throw ApiException.business("COMMENT_MODERATED", "Este comentário está em moderação.");
        }
        c.edit(ContentPolicy.check(req.body()), time.now());
        return new CommentView(c.getId(), names(List.of(userId)).get(userId), c.getBody(), c.getCreatedAt(), true, true);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        comments.delete(ownComment(userId, commentId));
    }

    @Transactional
    public LikeResult like(Long userId, Long postId) {
        visiblePost(postId);
        if (!likes.existsByPostIdAndUserId(postId, userId)) {
            likes.save(new ForumLike(postId, userId, time.now()));
        }
        return new LikeResult(true, likes.countByPostId(postId));
    }

    @Transactional
    public LikeResult unlike(Long userId, Long postId) {
        visiblePost(postId);
        likes.findByPostIdAndUserId(postId, userId).ifPresent(likes::delete);
        return new LikeResult(false, likes.countByPostId(postId));
    }

    @Transactional
    public void reportPost(Long userId, Long postId, ReportRequest req) {
        rateLimiter.check("forum-report:" + userId, 20, Duration.ofHours(1));
        ForumPost post = visiblePost(postId);
        if (post.getAuthorId().equals(userId)) {
            throw ApiException.business("OWN_CONTENT", "Você não pode denunciar sua própria publicação.");
        }
        if (reports.existsByReporterIdAndPostId(userId, postId)) {
            throw ApiException.conflict("ALREADY_REPORTED", "Você já denunciou esta publicação. A moderação vai analisar.");
        }
        reports.save(new ForumReport(userId, postId, null, req.reason(), ContentPolicy.sanitize(req.details()), time.now()));
        if (reports.countByPostIdAndStatus(postId, ReportStatus.OPEN) >= AUTO_HIDE_THRESHOLD) {
            post.setStatus(Status.HIDDEN, time.now());
        }
    }

    @Transactional
    public void reportComment(Long userId, Long commentId, ReportRequest req) {
        rateLimiter.check("forum-report:" + userId, 20, Duration.ofHours(1));
        ForumComment c = comments.findById(commentId).filter(x -> x.getStatus() == Status.VISIBLE)
                .orElseThrow(() -> ApiException.notFound("Comentário"));
        if (c.getAuthorId().equals(userId)) {
            throw ApiException.business("OWN_CONTENT", "Você não pode denunciar seu próprio comentário.");
        }
        if (reports.existsByReporterIdAndCommentId(userId, commentId)) {
            throw ApiException.conflict("ALREADY_REPORTED", "Você já denunciou este comentário. A moderação vai analisar.");
        }
        reports.save(new ForumReport(userId, null, commentId, req.reason(), ContentPolicy.sanitize(req.details()), time.now()));
        if (reports.countByCommentIdAndStatus(commentId, ReportStatus.OPEN) >= AUTO_HIDE_THRESHOLD) {
            c.setStatus(Status.HIDDEN, time.now());
        }
    }

    private ForumPost visiblePost(Long postId) {
        return posts.findByIdAndStatus(postId, Status.VISIBLE).orElseThrow(() -> ApiException.notFound("Publicação"));
    }

    private ForumPost ownPost(Long userId, Long postId) {
        ForumPost post = posts.findById(postId).filter(p -> p.getStatus() != Status.REMOVED)
                .orElseThrow(() -> ApiException.notFound("Publicação"));
        if (!post.getAuthorId().equals(userId)) {
            throw ApiException.forbidden("Somente o autor pode alterar esta publicação.");
        }
        return post;
    }

    private ForumComment ownComment(Long userId, Long commentId) {
        ForumComment c = comments.findById(commentId).filter(x -> x.getStatus() != Status.REMOVED)
                .orElseThrow(() -> ApiException.notFound("Comentário"));
        if (!c.getAuthorId().equals(userId)) {
            throw ApiException.forbidden("Somente o autor pode alterar este comentário.");
        }
        return c;
    }

    Map<Long, String> names(Collection<Long> ids) {
        return users.findAllById(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
    }

    private Map<ForumPost, PostSummary> summaries(Long userId, List<ForumPost> list) {
        Map<ForumPost, PostSummary> map = new HashMap<>();
        if (list.isEmpty()) {
            return map;
        }
        List<Long> ids = list.stream().map(ForumPost::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(likes.countByPosts(ids));
        Map<Long, Long> commentCounts = toCountMap(comments.countByPosts(ids, Status.VISIBLE));
        Set<Long> liked = new HashSet<>(likes.likedBy(userId, ids));
        Map<Long, String> names = names(list.stream().map(ForumPost::getAuthorId).toList());
        for (ForumPost p : list) {
            String excerpt = p.getBody().length() > 220 ? p.getBody().substring(0, 220) + "…" : p.getBody();
            map.put(p, new PostSummary(p.getId(), p.getCategory(), p.getTitle(), excerpt,
                    names.getOrDefault(p.getAuthorId(), "Usuário"), p.getCreatedAt(), p.isEdited(),
                    likeCounts.getOrDefault(p.getId(), 0L), commentCounts.getOrDefault(p.getId(), 0L),
                    liked.contains(p.getId()), p.getAuthorId().equals(userId)));
        }
        return map;
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(r -> map.put((Long) r[0], (Long) r[1]));
        return map;
    }
}

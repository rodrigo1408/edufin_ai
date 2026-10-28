package br.finedu.forum;

import br.finedu.forum.ForumEntities.Category;
import br.finedu.forum.ForumEntities.ReportReason;
import br.finedu.forum.ForumEntities.ReportStatus;
import br.finedu.forum.ForumEntities.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ForumDtos {

    private ForumDtos() {
    }

    public record PostRequest(@NotNull Category category,
                              @NotBlank @Size(min = 5, max = 140) String title,
                              @NotBlank @Size(min = 10, max = 5000) String body) {
    }

    public record CommentRequest(@NotBlank @Size(min = 2, max = 2000) String body) {
    }

    public record ReportRequest(@NotNull ReportReason reason, @Size(max = 500) String details) {
    }

    public record PostSummary(Long id, Category category, String title, String excerpt, String author,
                              Instant createdAt, boolean edited, long likes, long comments, boolean likedByMe,
                              boolean mine) {
    }

    public record CommentView(Long id, String author, String body, Instant createdAt, boolean edited, boolean mine) {
    }

    public record PostDetail(PostSummary post, String body) {
    }

    public record LikeResult(boolean liked, long likes) {
    }

    public record ReportView(Long id, String targetType, Long targetId, Long postId, String targetTitle,
                             String targetExcerpt, String targetAuthor, Status targetStatus, ReportReason reason,
                             String details, ReportStatus status, long openReportsOnTarget, Instant createdAt) {
    }

    public enum ModerationAction { HIDE, REMOVE, RESTORE, DISMISS }

    public record ModerationRequest(@NotNull ModerationAction action) {
    }

    public record ModeratedItem(Long id, String type, Long postId, String title, String excerpt, String author,
                                Status status, Instant updatedAt) {
    }
}

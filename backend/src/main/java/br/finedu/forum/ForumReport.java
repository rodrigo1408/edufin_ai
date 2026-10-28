package br.finedu.forum;

import br.finedu.forum.ForumEntities.ReportReason;
import br.finedu.forum.ForumEntities.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Denúncia de publicação OU de comentário (exatamente um dos dois). */
@Entity
@Table(name = "forum_reports")
public class ForumReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ForumReport() {
    }

    public ForumReport(Long reporterId, Long postId, Long commentId, ReportReason reason, String details, Instant now) {
        this.reporterId = reporterId;
        this.postId = postId;
        this.commentId = commentId;
        this.reason = reason;
        this.details = details;
        this.createdAt = now;
    }

    public void close(ReportStatus status, Long moderatorId, Instant now) {
        this.status = status;
        this.resolvedBy = moderatorId;
        this.resolvedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

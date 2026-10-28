package br.finedu.forum;

import br.finedu.forum.ForumEntities.ReportStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumReportRepository extends JpaRepository<ForumReport, Long> {

    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    long countByPostIdAndStatus(Long postId, ReportStatus status);

    long countByCommentIdAndStatus(Long commentId, ReportStatus status);

    Page<ForumReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    List<ForumReport> findByPostIdAndStatus(Long postId, ReportStatus status);

    List<ForumReport> findByCommentIdAndStatus(Long commentId, ReportStatus status);

    long countByStatus(ReportStatus status);
}

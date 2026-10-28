package br.finedu.feedback;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Feedback> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select f from Feedback f where (:status is null or f.status = :status)
              and (:type is null or f.type = :type) order by f.createdAt desc
            """)
    Page<Feedback> search(@Param("status") Feedback.Status status, @Param("type") Feedback.Type type,
                          Pageable pageable);

    long countByStatus(Feedback.Status status);
}

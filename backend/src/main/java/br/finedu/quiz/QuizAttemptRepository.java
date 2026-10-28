package br.finedu.quiz;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from QuizAttempt a where a.id = :id and a.userId = :userId")
    Optional<QuizAttempt> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<QuizAttempt> findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(Long userId, Long quizId,
                                                                                  QuizAttempt.Status status);

    List<QuizAttempt> findByUserIdAndQuizIdOrderByStartedAtDesc(Long userId, Long quizId);

    Page<QuizAttempt> findByUserIdAndStatusOrderByFinishedAtDesc(Long userId, QuizAttempt.Status status,
                                                                 Pageable pageable);

    List<QuizAttempt> findByUserIdAndStatusOrderByFinishedAtAsc(Long userId, QuizAttempt.Status status);

    @Query("""
            select a from QuizAttempt a where a.userId = :userId and a.status = :status
              and a.finishedAt >= :from and a.finishedAt < :to order by a.finishedAt asc
            """)
    List<QuizAttempt> findCompletedBetween(@Param("userId") Long userId, @Param("status") QuizAttempt.Status status,
                                           @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select a.quizId, max(a.percent), count(a) from QuizAttempt a
            where a.userId = :userId and a.status = :status group by a.quizId
            """)
    List<Object[]> bestByQuiz(@Param("userId") Long userId, @Param("status") QuizAttempt.Status status);

    boolean existsByQuizId(Long quizId);
}

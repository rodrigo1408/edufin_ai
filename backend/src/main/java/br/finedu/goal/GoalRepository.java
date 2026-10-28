package br.finedu.goal;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    /** Bloqueio pessimista: serializa movimentações concorrentes na mesma meta. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id = :id and g.userId = :userId")
    Optional<Goal> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<Goal.Status> statuses);

    List<Goal> findByStatusAndDeadlineIsNotNullAndDeadlineLessThanEqual(Goal.Status status, LocalDate limit);
}

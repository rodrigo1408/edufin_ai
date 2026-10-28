package br.finedu.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalMovementRepository extends JpaRepository<GoalMovement, Long> {

    @Query("""
            select coalesce(sum(case when m.type = :contribution then m.amount else -m.amount end), 0)
            from GoalMovement m where m.goalId = :goalId
            """)
    BigDecimal balance(@Param("goalId") Long goalId, @Param("contribution") GoalMovement.Type contribution);

    default BigDecimal balance(Long goalId) {
        return balance(goalId, GoalMovement.Type.CONTRIBUTION);
    }

    @Query("""
            select m.goalId, sum(case when m.type = :contribution then m.amount else -m.amount end)
            from GoalMovement m where m.goalId in :goalIds group by m.goalId
            """)
    List<Object[]> balances(@Param("goalIds") Collection<Long> goalIds,
                            @Param("contribution") GoalMovement.Type contribution);

    Page<GoalMovement> findByGoalIdOrderByCreatedAtDescIdDesc(Long goalId, Pageable pageable);

    /** Movimentações de todas as metas do usuário no período (para relatórios). */
    @Query("""
            select m from GoalMovement m, Goal g
            where m.goalId = g.id and g.userId = :userId and m.createdAt >= :from and m.createdAt < :to
            order by m.createdAt asc
            """)
    List<GoalMovement> findForUserBetween(@Param("userId") Long userId, @Param("from") Instant from,
                                          @Param("to") Instant to);

    @Query("select m from GoalMovement m, Goal g where m.goalId = g.id and g.userId = :userId order by m.createdAt asc")
    List<GoalMovement> findAllForUser(@Param("userId") Long userId);
}

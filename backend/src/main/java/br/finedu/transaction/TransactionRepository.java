package br.finedu.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findTop5ByUserIdOrderByOccurredOnDescIdDesc(Long userId);

    List<Transaction> findByUserIdOrderByOccurredOnAscIdAsc(Long userId);

    @Query("""
            select t.type, coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.occurredOn >= :from and t.occurredOn <= :to
            group by t.type
            """)
    List<Object[]> totalsByType(@Param("userId") Long userId, @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("""
            select t.category, coalesce(sum(t.amount), 0), count(t) from Transaction t
            where t.userId = :userId and t.type = :type and t.occurredOn >= :from and t.occurredOn <= :to
            group by t.category
            """)
    List<Object[]> totalsByCategory(@Param("userId") Long userId, @Param("type") Transaction.Type type,
                                    @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select t.occurredOn, t.type, coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.occurredOn >= :from and t.occurredOn <= :to
            group by t.occurredOn, t.type order by t.occurredOn
            """)
    List<Object[]> dailyTotals(@Param("userId") Long userId, @Param("from") LocalDate from,
                               @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(case when t.type = :income then t.amount else -t.amount end), 0)
            from Transaction t where t.userId = :userId and t.occurredOn < :before
            """)
    BigDecimal balanceBefore(@Param("userId") Long userId, @Param("before") LocalDate before,
                             @Param("income") Transaction.Type income);

    long countByUserId(Long userId);
}

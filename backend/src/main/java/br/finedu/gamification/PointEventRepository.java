package br.finedu.gamification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointEventRepository extends JpaRepository<PointEvent, Long> {

    boolean existsByUserIdAndEventTypeAndReferenceKey(Long userId, PointRule type, String referenceKey);

    long countByUserIdAndEventType(Long userId, PointRule type);

    long countByUserIdAndEventTypeAndCreatedAtGreaterThanEqual(Long userId, PointRule type, Instant from);

    @Query("select p.eventType, count(p) from PointEvent p where p.userId = :userId group by p.eventType")
    List<Object[]> countsByType(@Param("userId") Long userId);

    @Query("select coalesce(sum(p.points), 0) from PointEvent p where p.userId = :userId")
    long totalPoints(@Param("userId") Long userId);

    @Query("select coalesce(sum(p.points), 0) from PointEvent p where p.userId = :userId and p.createdAt >= :from")
    long pointsSince(@Param("userId") Long userId, @Param("from") Instant from);

    @Query("select p.createdAt from PointEvent p where p.userId = :userId and p.eventType in :types")
    List<Instant> activityInstants(@Param("userId") Long userId, @Param("types") Collection<PointRule> types);

    @Query("select p.createdAt from PointEvent p where p.userId = :userId and p.eventType = :type and p.createdAt >= :from")
    List<Instant> instantsSince(@Param("userId") Long userId, @Param("type") PointRule type,
                                @Param("from") Instant from);

    Page<PointEvent> findByUserIdAndPointsGreaterThanOrderByCreatedAtDescIdDesc(Long userId, int points,
                                                                                Pageable pageable);

    List<PointEvent> findByUserIdOrderByCreatedAtAsc(Long userId);
}

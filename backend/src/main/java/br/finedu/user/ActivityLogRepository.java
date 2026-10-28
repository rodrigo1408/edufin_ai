package br.finedu.user;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    List<ActivityLog> findTop8ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<ActivityLog> findByUserIdOrderByCreatedAtAsc(Long userId);

    @Modifying
    @Query("delete from ActivityLog a where a.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}

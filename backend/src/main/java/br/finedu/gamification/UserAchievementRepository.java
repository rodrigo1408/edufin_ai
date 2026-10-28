package br.finedu.gamification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    @Query("select ua from UserAchievement ua join fetch ua.achievement where ua.userId = :userId order by ua.unlockedAt desc")
    List<UserAchievement> findByUser(@Param("userId") Long userId);

    @Query("select ua.achievement.code from UserAchievement ua where ua.userId = :userId")
    List<String> unlockedCodes(@Param("userId") Long userId);

    long countByUserId(Long userId);
}

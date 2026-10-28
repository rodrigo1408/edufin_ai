package br.finedu.gamification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByOrderByIdAsc();
}

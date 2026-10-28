package br.finedu.course;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

    long countByUserId(Long userId);

    @Query("""
            select m.course.id, count(p) from LessonProgress p, Lesson l join l.module m
            where p.lessonId = l.id and p.userId = :userId group by m.course.id
            """)
    List<Object[]> completedCountByCourse(@Param("userId") Long userId);

    List<LessonProgress> findByUserIdOrderByCompletedAtAsc(Long userId);
}

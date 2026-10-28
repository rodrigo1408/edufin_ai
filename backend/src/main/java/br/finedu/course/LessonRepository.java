package br.finedu.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("select l from Lesson l join fetch l.module m join fetch m.course c where l.id = :id")
    Optional<Lesson> findWithCourse(@Param("id") Long id);

    @Query("select l from Lesson l join fetch l.module m join fetch m.course c where l.quizId = :quizId")
    List<Lesson> findByQuizId(@Param("quizId") Long quizId);

    @Query("select count(l) from Lesson l where l.module.course.id = :courseId")
    long countByCourse(@Param("courseId") Long courseId);
}

package br.finedu.course;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    long countByUserIdAndCompletedAtIsNotNull(Long userId);

    long countByCourseId(Long courseId);

    @Query("select e from Enrollment e where e.userId = :userId and e.completedAt >= :from and e.completedAt < :to")
    List<Enrollment> completedBetween(@Param("userId") Long userId, @Param("from") Instant from,
                                      @Param("to") Instant to);

    /** Usuários com cursos em andamento (para lembretes de estudo). */
    @Query("select distinct e.userId from Enrollment e where e.completedAt is null")
    List<Long> usersWithCoursesInProgress();
}

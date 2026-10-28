package br.finedu.course;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    @Query("select m from CourseModule m join fetch m.course where m.id = :id")
    Optional<CourseModule> findWithCourse(@Param("id") Long id);
}

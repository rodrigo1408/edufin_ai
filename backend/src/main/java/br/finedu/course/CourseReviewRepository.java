package br.finedu.course;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByUserIdAndCourseId(Long userId, Long courseId);

    Page<CourseReview> findByCourseIdAndHiddenFalseOrderByCreatedAtDesc(Long courseId, Pageable pageable);

    Page<CourseReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select r.courseId, avg(r.rating), count(r) from CourseReview r group by r.courseId")
    List<Object[]> ratingStats();

    List<CourseReview> findByUserId(Long userId);
}

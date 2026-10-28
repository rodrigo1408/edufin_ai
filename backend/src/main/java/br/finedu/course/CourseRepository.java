package br.finedu.course;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
            select c from Course c where c.published = true
              and (:topic is null or c.category = :topic)
              and (:difficulty is null or c.difficulty = :difficulty)
            order by c.position, c.id
            """)
    List<Course> findPublished(@Param("topic") Topic topic, @Param("difficulty") Difficulty difficulty);

    List<Course> findByPublishedTrueAndCategoryInOrderByPositionAsc(Collection<Topic> topics);

    Optional<Course> findByIdAndPublishedTrue(Long id);

    List<Course> findAllByOrderByPositionAscIdAsc();

    boolean existsBySlug(String slug);
}

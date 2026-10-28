package br.finedu.quiz;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("""
            select q from Quiz q where q.published = true
              and (:topic is null or q.category = :topic)
              and (:difficulty is null or q.difficulty = :difficulty)
            order by q.difficulty, q.id
            """)
    List<Quiz> findPublished(@Param("topic") Topic topic, @Param("difficulty") Difficulty difficulty);

    Optional<Quiz> findByIdAndPublishedTrue(Long id);

    List<Quiz> findAllByOrderByIdAsc();

    @Query("select q.id, count(qq) from Quiz q left join q.questions qq group by q.id")
    List<Object[]> questionCounts();
}

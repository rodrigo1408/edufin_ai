package br.finedu.quiz;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query("select q from QuizQuestion q join fetch q.quiz z where q.id = :id and z.published = true")
    Optional<QuizQuestion> findPublished(@Param("id") Long id);
}

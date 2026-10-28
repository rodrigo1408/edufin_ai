package br.finedu.quiz;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {

    List<QuizAttemptAnswer> findByAttemptId(Long attemptId);

    boolean existsByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    long countByAttemptIdAndCorrectTrue(Long attemptId);
}

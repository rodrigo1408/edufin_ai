package br.finedu.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Resposta registrada. A correção é determinada no servidor no momento da resposta. */
@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected QuizAttemptAnswer() {
    }

    public QuizAttemptAnswer(Long attemptId, Long questionId, Long optionId, boolean correct, Instant answeredAt) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.optionId = optionId;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public boolean isCorrect() {
        return correct;
    }
}

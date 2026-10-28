package br.finedu.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    public enum Status { IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "correct_count")
    private Integer correctCount;

    private Integer score;

    @Column(precision = 5, scale = 2)
    private BigDecimal percent;

    protected QuizAttempt() {
    }

    public QuizAttempt(Long userId, Long quizId, int totalQuestions, Instant startedAt) {
        this.userId = userId;
        this.quizId = quizId;
        this.totalQuestions = totalQuestions;
        this.startedAt = startedAt;
    }

    public void complete(QuizScoring.Score s, Instant now) {
        this.status = Status.COMPLETED;
        this.finishedAt = now;
        this.correctCount = s.correct();
        this.score = s.points();
        this.percent = s.percent();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public Integer getScore() {
        return score;
    }

    public BigDecimal getPercent() {
        return percent;
    }
}

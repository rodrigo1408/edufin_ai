package br.finedu.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(Long userId, Long lessonId, Instant completedAt) {
        this.userId = userId;
        this.lessonId = lessonId;
        this.completedAt = completedAt;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

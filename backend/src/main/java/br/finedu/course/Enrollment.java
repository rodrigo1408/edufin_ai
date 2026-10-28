package br.finedu.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Última aula acessada, usada para "continuar de onde parou". */
    @Column(name = "last_lesson_id")
    private Long lastLessonId;

    protected Enrollment() {
    }

    public Enrollment(Long userId, Long courseId, Instant now) {
        this.userId = userId;
        this.courseId = courseId;
        this.enrolledAt = now;
    }

    public void touch(Long lessonId) {
        this.lastLessonId = lessonId;
    }

    public boolean complete(Instant now) {
        if (completedAt != null) {
            return false;
        }
        completedAt = now;
        return true;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Long getLastLessonId() {
        return lastLessonId;
    }
}

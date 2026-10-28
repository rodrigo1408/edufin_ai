package br.finedu.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "course_reviews")
public class CourseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    /** Ocultada pela moderação (não aparece publicamente, mas segue na média). */
    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CourseReview() {
    }

    public CourseReview(Long userId, Long courseId, Instant now) {
        this.userId = userId;
        this.courseId = courseId;
        this.createdAt = now;
    }

    public void update(int rating, String comment, Instant now) {
        this.rating = rating;
        this.comment = comment;
        this.updatedAt = now;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
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

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package br.finedu.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Anotação pessoal (privada) do usuário sobre uma aula. */
@Entity
@Table(name = "lesson_notes")
public class LessonNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonNote() {
    }

    public LessonNote(Long userId, Long lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }

    public void write(String content, Instant now) {
        this.content = content;
        this.updatedAt = now;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

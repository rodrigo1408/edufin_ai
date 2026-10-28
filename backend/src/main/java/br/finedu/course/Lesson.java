package br.finedu.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Aula com conteúdo textual em um subconjunto simples de Markdown (títulos, listas, negrito,
 * citações). O frontend renderiza esse conteúdo como texto seguro, sem HTML bruto.
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 20000)
    private String content;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "quiz_id")
    private Long quizId;

    protected Lesson() {
    }

    public Lesson(CourseModule module, int position) {
        this.module = module;
        this.position = position;
    }

    public void update(String title, String content, int estimatedMinutes, Long quizId) {
        this.title = title;
        this.content = content;
        this.estimatedMinutes = estimatedMinutes;
        this.quizId = quizId;
    }

    public Long getId() {
        return id;
    }

    public CourseModule getModule() {
        return module;
    }

    public int getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public Long getQuizId() {
        return quizId;
    }
}

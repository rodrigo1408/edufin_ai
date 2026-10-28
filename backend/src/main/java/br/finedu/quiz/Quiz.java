package br.finedu.quiz;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Topic category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Difficulty difficulty;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    protected Quiz() {
    }

    public Quiz(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, Topic category, Difficulty difficulty, boolean published,
                       Instant now) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.published = published;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Topic getCategory() {
        return category;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public boolean isPublished() {
        return published;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}

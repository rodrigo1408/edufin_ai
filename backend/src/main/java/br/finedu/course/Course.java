package br.finedu.course;

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
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Objetivos de aprendizagem, um por linha. */
    @Column(nullable = false, length = 2000)
    private String objectives;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Topic category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Difficulty difficulty;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    /** Ordem sugerida na trilha (progressão do iniciante ao avançado). */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CourseModule> modules = new ArrayList<>();

    protected Course() {
    }

    public Course(String slug, Instant now) {
        this.slug = slug;
        this.createdAt = now;
    }

    public void update(String title, String summary, String description, String objectives, Topic category,
                       Difficulty difficulty, int estimatedMinutes, int position, boolean published, Instant now) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.objectives = objectives;
        this.category = category;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.position = position;
        this.published = published;
        this.updatedAt = now;
    }

    public List<Lesson> orderedLessons() {
        return modules.stream().flatMap(m -> m.getLessons().stream()).toList();
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getObjectives() {
        return objectives;
    }

    public Topic getCategory() {
        return category;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPublished() {
        return published;
    }

    public List<CourseModule> getModules() {
        return modules;
    }
}

package br.finedu.forum;

import br.finedu.forum.ForumEntities.Category;
import br.finedu.forum.ForumEntities.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "forum_posts")
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.VISIBLE;

    @Column(nullable = false)
    private boolean edited;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ForumPost() {
    }

    public ForumPost(Long authorId, Category category, String title, String body, Instant now) {
        this.authorId = authorId;
        this.category = category;
        this.title = title;
        this.body = body;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void edit(Category category, String title, String body, Instant now) {
        this.category = category;
        this.title = title;
        this.body = body;
        this.edited = true;
        this.updatedAt = now;
    }

    public void setStatus(Status status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public Category getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isEdited() {
        return edited;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

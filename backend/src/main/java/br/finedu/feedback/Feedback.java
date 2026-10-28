package br.finedu.feedback;

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
@Table(name = "feedback")
public class Feedback {

    public enum Type { SUGGESTION, PROBLEM, EXPERIENCE }

    public enum Status { NEW, IN_REVIEW, RESOLVED, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fica nulo se a conta for excluída (o feedback permanece anonimizado). */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.NEW;

    @Column(name = "admin_response", length = 1000)
    private String adminResponse;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Feedback() {
    }

    public Feedback(Long userId, Type type, String subject, String message, Instant now) {
        this.userId = userId;
        this.type = type;
        this.subject = subject;
        this.message = message;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void handle(Status status, String response, Long adminId, Instant now) {
        this.status = status;
        this.adminResponse = response;
        this.handledBy = adminId;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

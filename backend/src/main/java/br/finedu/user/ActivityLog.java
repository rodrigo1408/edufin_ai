package br.finedu.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Histórico de atividades relevantes, visível apenas para o próprio usuário. */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    public enum Type {
        ACCOUNT_CREATED, LOGIN, PROFILE_UPDATED, PASSWORD_CHANGED, PASSWORD_RESET, PREFERENCES_UPDATED,
        GOAL_CREATED, GOAL_UPDATED, GOAL_CANCELLED, GOAL_COMPLETED, GOAL_MOVEMENT,
        TRANSACTION_CREATED, TRANSACTION_UPDATED, TRANSACTION_DELETED,
        SIMULATION_SAVED, QUIZ_COMPLETED, COURSE_ENROLLED, LESSON_COMPLETED, COURSE_COMPLETED,
        ACHIEVEMENT_UNLOCKED, POST_CREATED, WALLET_CREATED, WALLET_MOVEMENT, FEEDBACK_SENT, DATA_EXPORTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Type type;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivityLog() {
    }

    public ActivityLog(Long userId, Type type, String description, Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.description = description.length() > 255 ? description.substring(0, 255) : description;
        this.createdAt = createdAt;
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

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package br.finedu.goal;

import br.finedu.common.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Meta financeira. O valor acumulado NÃO é armazenado aqui: é sempre a soma das movimentações
 * ({@link GoalMovement}), evitando agregados duplicados que poderiam divergir do histórico.
 */
@Entity
@Table(name = "goals")
public class Goal {

    public enum Status { ACTIVE, COMPLETED, CANCELLED }

    public enum Category { ELECTRONICS, EDUCATION, TRAVEL, EMERGENCY_FUND, LEISURE, GIFT, CLOTHING, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(name = "target_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal targetAmount;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    protected Goal() {
    }

    public Goal(Long userId, String title, String description, Category category, BigDecimal targetAmount,
                LocalDate deadline, Instant now) {
        this.userId = userId;
        this.createdAt = now;
        update(title, description, category, targetAmount, deadline, now);
    }

    public void update(String title, String description, Category category, BigDecimal targetAmount,
                       LocalDate deadline, Instant now) {
        requireNotCancelled();
        this.title = title;
        this.description = description;
        this.category = category;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
        this.updatedAt = now;
    }

    /**
     * Recalcula o status a partir do saldo: conclui automaticamente ao atingir o alvo e reabre se o
     * saldo voltar a ficar abaixo dele (retirada ou aumento do alvo).
     *
     * @return true se a meta acabou de ser concluída
     */
    public boolean refreshStatus(BigDecimal saved, Instant now) {
        if (status == Status.CANCELLED) {
            return false;
        }
        boolean reached = saved.compareTo(targetAmount) >= 0;
        if (reached && status == Status.ACTIVE) {
            status = Status.COMPLETED;
            completedAt = now;
            return true;
        }
        if (!reached && status == Status.COMPLETED) {
            status = Status.ACTIVE;
            completedAt = null;
        }
        return false;
    }

    public void cancel(Instant now) {
        requireNotCancelled();
        status = Status.CANCELLED;
        cancelledAt = now;
        updatedAt = now;
    }

    public void requireNotCancelled() {
        if (status == Status.CANCELLED) {
            throw ApiException.business("GOAL_CANCELLED", "Esta meta foi cancelada e não pode ser alterada.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}

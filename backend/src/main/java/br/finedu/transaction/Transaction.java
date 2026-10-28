package br.finedu.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    public enum Type { INCOME, EXPENSE }

    /**
     * Origem do registro: MANUAL = registro educativo feito pelo próprio usuário; SIMULATED =
     * exercício/simulação (não representa dinheiro real); EXTERNAL = reservado para integrações
     * futuras (somente leitura, não pode ser criado nem alterado pela API).
     */
    public enum Origin { MANUAL, SIMULATED, EXTERNAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionCategory category;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origin origin;

    @Column(name = "goal_id")
    private Long goalId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(Long userId, Origin origin, Instant now) {
        this.userId = userId;
        this.origin = origin;
        this.createdAt = now;
    }

    public void apply(String description, BigDecimal amount, Type type, TransactionCategory category,
                      LocalDate occurredOn, String notes, Long goalId, Origin origin, Instant now) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.occurredOn = occurredOn;
        this.notes = notes;
        this.goalId = goalId;
        this.origin = origin;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Type getType() {
        return type;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getNotes() {
        return notes;
    }

    public Origin getOrigin() {
        return origin;
    }

    public Long getGoalId() {
        return goalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

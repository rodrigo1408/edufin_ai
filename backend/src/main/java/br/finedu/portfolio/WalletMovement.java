package br.finedu.portfolio;

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
@Table(name = "wallet_movements")
public class WalletMovement {

    public enum Type {
        INITIAL, DEPOSIT, WITHDRAWAL, SIMULATED_RETURN;

        public boolean isCredit() {
            return this != WITHDRAWAL;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(length = 200)
    private String note;

    /** Mês (yyyy-MM) do rendimento simulado; único por carteira, impedindo rendimento duplicado. */
    @Column(name = "reference_period", length = 7)
    private String referencePeriod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WalletMovement() {
    }

    public WalletMovement(Long walletId, Type type, BigDecimal amount, LocalDate occurredOn, String note,
                          String referencePeriod, Instant createdAt) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.note = note;
        this.referencePeriod = referencePeriod;
        this.createdAt = createdAt;
    }

    public BigDecimal signedAmount() {
        return type.isCredit() ? amount : amount.negate();
    }

    public Long getId() {
        return id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public String getNote() {
        return note;
    }

    public String getReferencePeriod() {
        return referencePeriod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

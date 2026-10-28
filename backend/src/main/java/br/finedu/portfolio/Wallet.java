package br.finedu.portfolio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Carteira de investimento SIMULADA, para fins educativos. Não movimenta dinheiro real, não se
 * conecta a instituições financeiras e não usa cotações de mercado: a rentabilidade é uma taxa
 * hipotética escolhida pelo próprio usuário.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 300)
    private String description;

    /** Taxa anual hipotética (%), usada apenas para gerar rendimentos simulados. */
    @Column(name = "assumed_annual_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal assumedAnnualRate;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Wallet() {
    }

    public Wallet(Long userId, String name, String description, BigDecimal assumedAnnualRate, Instant now) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.assumedAnnualRate = assumedAnnualRate;
        this.createdAt = now;
    }

    public void update(String name, String description, BigDecimal assumedAnnualRate) {
        this.name = name;
        this.description = description;
        this.assumedAnnualRate = assumedAnnualRate;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAssumedAnnualRate() {
        return assumedAnnualRate;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

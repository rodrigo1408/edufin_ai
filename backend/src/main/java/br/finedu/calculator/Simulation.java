package br.finedu.calculator;

import br.finedu.calculator.InterestCalculator.InterestType;
import br.finedu.calculator.InterestCalculator.PeriodUnit;
import br.finedu.calculator.InterestCalculator.RateUnit;
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

/** Simulação salva. Os resultados são sempre calculados pelo servidor no momento do salvamento. */
@Entity
@Table(name = "simulations")
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 10)
    private InterestType interestType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal principal;

    @Column(name = "rate_percent", nullable = false, precision = 9, scale = 4)
    private BigDecimal ratePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false, length = 10)
    private RateUnit rateUnit;

    @Column(nullable = false)
    private int period;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_unit", nullable = false, length = 10)
    private PeriodUnit periodUnit;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal contribution;

    @Column(name = "final_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "total_invested", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "total_interest", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Simulation() {
    }

    public Simulation(Long userId, String name, InterestCalculator.Input in, InterestCalculator.Result r,
                      Instant now) {
        this.userId = userId;
        this.name = name;
        this.interestType = in.interestType();
        this.principal = in.principal();
        this.ratePercent = in.ratePercent();
        this.rateUnit = in.rateUnit();
        this.period = in.period();
        this.periodUnit = in.periodUnit();
        this.contribution = in.monthlyContribution();
        this.finalAmount = r.finalAmount();
        this.totalInvested = r.totalInvested();
        this.totalInterest = r.totalInterest();
        this.createdAt = now;
    }

    public InterestCalculator.Input toInput() {
        return new InterestCalculator.Input(interestType, principal, ratePercent, rateUnit, period, periodUnit,
                contribution);
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

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

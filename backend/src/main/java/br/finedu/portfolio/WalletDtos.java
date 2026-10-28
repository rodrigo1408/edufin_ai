package br.finedu.portfolio;

import br.finedu.calculator.InterestCalculator;
import br.finedu.gamification.GamificationService.Reward;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class WalletDtos {

    public static final String DISCLAIMER = "Carteira SIMULADA para fins educativos: nenhum valor é real, não há "
            + "conexão com bancos ou corretoras e os rendimentos usam uma taxa hipotética escolhida por você. "
            + "Rentabilidade passada ou simulada não garante resultados futuros.";

    private WalletDtos() {
    }

    public record WalletRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 300) String description,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 4) BigDecimal assumedAnnualRate,
            @DecimalMin("0.00") @DecimalMax("999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal initialBalance) {
    }

    public enum ManualType { DEPOSIT, WITHDRAWAL }

    public record MovementRequest(
            @NotNull ManualType type,
            @NotNull @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            LocalDate occurredOn,
            @Size(max = 200) String note) {
    }

    public record ReturnRequest(@NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Use o formato AAAA-MM.")
                                String period) {
    }

    public record ScenarioRequest(
            @Min(1) @Max(360) int months,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer = 10, fraction = 2) BigDecimal monthlyContribution,
            @NotNull @Size(min = 1, max = 4) List<@NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal> annualRates) {
    }

    public record WalletSummary(Long id, String name, String description, BigDecimal assumedAnnualRate,
                                BigDecimal balance, boolean archived, Instant createdAt, boolean simulated) {
    }

    public record MovementView(Long id, WalletMovement.Type type, BigDecimal amount, LocalDate occurredOn, String note,
                               String referencePeriod, Instant createdAt) {
        static MovementView of(WalletMovement m) {
            return new MovementView(m.getId(), m.getType(), m.getAmount(), m.getOccurredOn(), m.getNote(),
                    m.getReferencePeriod(), m.getCreatedAt());
        }
    }

    public record BalancePoint(LocalDate date, BigDecimal balance) {
    }

    public record Performance(BigDecimal initial, BigDecimal deposits, BigDecimal withdrawals,
                              BigDecimal simulatedReturns, BigDecimal netContributed, BigDecimal balance,
                              BigDecimal returnPercent) {
    }

    public record WalletDetail(WalletSummary wallet, Performance performance, List<BalancePoint> evolution,
                               String disclaimer) {
    }

    public record ScenarioResult(BigDecimal annualRate, InterestCalculator.Result result) {
    }

    public record ScenarioResponse(BigDecimal startingBalance, int months, BigDecimal monthlyContribution,
                                   List<ScenarioResult> scenarios, String disclaimer) {
    }

    public record CreateResult(WalletDetail wallet, Reward reward) {
    }
}

package br.finedu.calculator;

import br.finedu.calculator.InterestCalculator.InterestType;
import br.finedu.calculator.InterestCalculator.PeriodUnit;
import br.finedu.calculator.InterestCalculator.RateUnit;
import br.finedu.calculator.InterestCalculator.Result;
import br.finedu.gamification.GamificationService.Reward;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CalculatorDtos {

    private CalculatorDtos() {
    }

    public record InputDto(
            @NotNull InterestType interestType,
            @NotNull @DecimalMin("0.00") @DecimalMax("999999999.99") @Digits(integer = 12, fraction = 2)
            BigDecimal principal,
            @NotNull @DecimalMin("0.00") @DecimalMax(value = "100.00", message = "A taxa deve ser no máximo 100%.")
            @Digits(integer = 3, fraction = 4) BigDecimal ratePercent,
            @NotNull RateUnit rateUnit,
            @Min(1) @Max(600) int period,
            @NotNull PeriodUnit periodUnit,
            @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer = 10, fraction = 2)
            BigDecimal monthlyContribution) {

        InterestCalculator.Input toInput() {
            return new InterestCalculator.Input(interestType, principal, ratePercent, rateUnit, period, periodUnit,
                    monthlyContribution);
        }

        static InputDto of(InterestCalculator.Input i) {
            return new InputDto(i.interestType(), i.principal(), i.ratePercent(), i.rateUnit(), i.period(),
                    i.periodUnit(), i.monthlyContribution());
        }
    }

    public record CalculationResponse(InputDto input, Result result, List<String> assumptions) {
    }

    public record CompareRequest(@NotNull @Size(min = 2, max = 4) List<@Valid @NotNull InputDto> scenarios) {
    }

    public record SaveRequest(@NotBlank @Size(max = 80) String name, @NotNull @Valid InputDto input) {
    }

    public record SimulationView(Long id, String name, InputDto input, BigDecimal finalAmount,
                                 BigDecimal totalInvested, BigDecimal totalInterest, Instant createdAt) {
    }

    public record SaveResponse(SimulationView simulation, Reward reward) {
    }
}

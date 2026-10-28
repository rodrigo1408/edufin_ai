package br.finedu.goal;

import br.finedu.gamification.GamificationService.Reward;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class GoalDtos {

    private GoalDtos() {
    }

    public record GoalRequest(
            @NotBlank @Size(max = 80) String title,
            @Size(max = 500) String description,
            @NotNull Goal.Category category,
            @NotNull @DecimalMin(value = "0.01", message = "O valor da meta deve ser maior que zero.")
            @DecimalMax(value = "999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal targetAmount,
            LocalDate deadline) {
    }

    public record MovementRequest(
            @NotNull GoalMovement.Type type,
            @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            @DecimalMax(value = "999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 200) String note) {
    }

    public record GoalView(Long id, String title, String description, Goal.Category category,
                           BigDecimal targetAmount, BigDecimal savedAmount, BigDecimal remainingAmount,
                           BigDecimal progressPercent, LocalDate deadline, Long daysRemaining,
                           BigDecimal suggestedMonthlySaving, Goal.Status status, Instant createdAt,
                           Instant completedAt) {
    }

    public record MovementView(Long id, GoalMovement.Type type, BigDecimal amount, String note, Instant createdAt) {
        static MovementView of(GoalMovement m) {
            return new MovementView(m.getId(), m.getType(), m.getAmount(), m.getNote(), m.getCreatedAt());
        }
    }

    public record MovementResult(GoalView goal, MovementView movement, boolean goalCompleted, Reward reward) {
    }

    public record GoalResult(GoalView goal, Reward reward) {
    }
}

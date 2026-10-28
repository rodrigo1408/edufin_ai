package br.finedu.transaction;

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
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotBlank @Size(max = 120) String description,
            @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            @DecimalMax("999999999.99") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @NotNull Transaction.Type type,
            @NotNull TransactionCategory category,
            @NotNull LocalDate occurredOn,
            @Size(max = 500) String notes,
            Transaction.Origin origin,
            Long goalId) {
    }

    public record TransactionView(Long id, String description, BigDecimal amount, Transaction.Type type,
                                  TransactionCategory category, String categoryLabel, LocalDate occurredOn,
                                  String notes, Transaction.Origin origin, Long goalId, Instant createdAt) {
        public static TransactionView of(Transaction t) {
            return new TransactionView(t.getId(), t.getDescription(), t.getAmount(), t.getType(), t.getCategory(),
                    t.getCategory().label(), t.getOccurredOn(), t.getNotes(), t.getOrigin(), t.getGoalId(),
                    t.getCreatedAt());
        }
    }

    public record TransactionResult(TransactionView transaction, Reward reward) {
    }

    public record CategoryTotal(TransactionCategory category, String label, BigDecimal total, long count) {
    }

    public record Summary(LocalDate from, LocalDate to, BigDecimal income, BigDecimal expense, BigDecimal balance,
                          List<CategoryTotal> expensesByCategory, List<CategoryTotal> incomeByCategory) {
    }

    public record CategoryOption(TransactionCategory value, String label, Transaction.Type type) {
    }
}

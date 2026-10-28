package br.finedu.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void progressIsProportionalAndRoundedToOneDecimal() {
        assertThat(Money.progressPercent(new BigDecimal("33.33"), new BigDecimal("100"))).isEqualByComparingTo("33.3");
        assertThat(Money.progressPercent(new BigDecimal("1"), new BigDecimal("3"))).isEqualByComparingTo("33.3");
        assertThat(Money.progressPercent(new BigDecimal("2"), new BigDecimal("3"))).isEqualByComparingTo("66.7");
    }

    @Test
    void progressHandlesZeroTargetAndOverflow() {
        assertThat(Money.progressPercent(new BigDecimal("50"), BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(Money.progressPercent(new BigDecimal("50"), null)).isEqualByComparingTo("0");
        assertThat(Money.progressPercent(new BigDecimal("250"), new BigDecimal("100"))).isEqualByComparingTo("100");
        assertThat(Money.progressPercent(BigDecimal.ZERO, new BigDecimal("100"))).isEqualByComparingTo("0");
    }

    @Test
    void requirePositiveRejectsInvalidAmounts() {
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("-1"), "Valor")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Money.requirePositive(BigDecimal.ZERO, "Valor")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("1.001"), "Valor"))
                .hasMessageContaining("2 casas");
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("1000000000"), "Valor"))
                .hasMessageContaining("limite");
        assertThat(Money.requirePositive(new BigDecimal("10.5"), "Valor")).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void decimalArithmeticHasNoFloatingPointDrift() {
        BigDecimal sum = Money.zero();
        for (int i = 0; i < 10; i++) {
            sum = sum.add(new BigDecimal("0.10"));
        }
        assertThat(sum).isEqualByComparingTo("1.00");
    }
}

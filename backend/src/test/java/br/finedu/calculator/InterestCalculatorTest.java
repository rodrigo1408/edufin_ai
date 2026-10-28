package br.finedu.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.finedu.calculator.InterestCalculator.Input;
import br.finedu.calculator.InterestCalculator.InterestType;
import br.finedu.calculator.InterestCalculator.PeriodUnit;
import br.finedu.calculator.InterestCalculator.RateUnit;
import br.finedu.calculator.InterestCalculator.Result;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class InterestCalculatorTest {

    private static Input in(InterestType type, String principal, String rate, RateUnit ru, int period, PeriodUnit pu,
                            String pmt) {
        return new Input(type, new BigDecimal(principal), new BigDecimal(rate), ru, period, pu, new BigDecimal(pmt));
    }

    @Test
    void simpleInterestWithoutContributions() {
        Result r = InterestCalculator.calculate(in(InterestType.SIMPLE, "100", "10", RateUnit.MONTHLY, 2,
                PeriodUnit.MONTHS, "0"));
        assertThat(r.finalAmount()).isEqualByComparingTo("120.00");
        assertThat(r.totalInvested()).isEqualByComparingTo("100.00");
        assertThat(r.totalInterest()).isEqualByComparingTo("20.00");
    }

    @Test
    void compoundInterestWithoutContributions() {
        Result r = InterestCalculator.calculate(in(InterestType.COMPOUND, "100", "10", RateUnit.MONTHLY, 2,
                PeriodUnit.MONTHS, "0"));
        assertThat(r.finalAmount()).isEqualByComparingTo("121.00");
        assertThat(r.totalInterest()).isEqualByComparingTo("21.00");
    }

    @Test
    void compoundIterationMatchesClosedFormWithContributions() {
        Result r = InterestCalculator.calculate(in(InterestType.COMPOUND, "1000", "1", RateUnit.MONTHLY, 24,
                PeriodUnit.MONTHS, "200"));
        BigDecimal expected = InterestCalculator.compoundClosedForm(new BigDecimal("1000"), new BigDecimal("200"),
                new BigDecimal("0.01"), 24).setScale(2, RoundingMode.HALF_EVEN);
        assertThat(r.finalAmount()).isEqualByComparingTo(expected);
        // Valor de referência: 1000·1,01^24 + 200·((1,01^24 − 1)/0,01) = 1269,7346 + 5394,6930 = 6664,4276
        assertThat(r.finalAmount()).isEqualByComparingTo("6664.43");
        assertThat(r.totalInvested()).isEqualByComparingTo("5800.00");
    }

    @Test
    void simpleInterestContributionsEarnOnlyForTimeInvested() {
        // P=0, PMT=100, i=1% a.m., 3 meses: 100·(1+0,02) + 100·(1+0,01) + 100 = 303
        Result r = InterestCalculator.calculate(in(InterestType.SIMPLE, "0", "1", RateUnit.MONTHLY, 3,
                PeriodUnit.MONTHS, "100"));
        assertThat(r.finalAmount()).isEqualByComparingTo("303.00");
    }

    @Test
    void yearlyRateUsesEquivalentMonthlyRateForCompound() {
        BigDecimal monthly = InterestCalculator.monthlyRate(InterestType.COMPOUND, new BigDecimal("12"), RateUnit.YEARLY);
        assertThat(monthly.setScale(6, RoundingMode.HALF_EVEN)).isEqualByComparingTo("0.009489");
        Result r = InterestCalculator.calculate(in(InterestType.COMPOUND, "1000", "12", RateUnit.YEARLY, 1,
                PeriodUnit.YEARS, "0"));
        assertThat(r.finalAmount()).isEqualByComparingTo("1120.00");
        assertThat(r.months()).isEqualTo(12);
    }

    @Test
    void yearlyRateUsesProportionalMonthlyRateForSimple() {
        Result r = InterestCalculator.calculate(in(InterestType.SIMPLE, "1000", "12", RateUnit.YEARLY, 6,
                PeriodUnit.MONTHS, "0"));
        assertThat(r.finalAmount()).isEqualByComparingTo("1060.00");
    }

    @Test
    void zeroRateKeepsOnlyInvestedValue() {
        Result r = InterestCalculator.calculate(in(InterestType.COMPOUND, "50", "0", RateUnit.MONTHLY, 10,
                PeriodUnit.MONTHS, "10"));
        assertThat(r.finalAmount()).isEqualByComparingTo("150.00");
        assertThat(r.totalInterest()).isEqualByComparingTo("0.00");
    }

    @Test
    void seriesIsMonthlyUpTo120MonthsAndYearlyBeyond() {
        Result shortTerm = InterestCalculator.calculate(in(InterestType.COMPOUND, "100", "1", RateUnit.MONTHLY, 12,
                PeriodUnit.MONTHS, "0"));
        assertThat(shortTerm.series()).hasSize(13);
        assertThat(shortTerm.series().get(0).balance()).isEqualByComparingTo("100.00");
        Result longTerm = InterestCalculator.calculate(in(InterestType.COMPOUND, "100", "1", RateUnit.MONTHLY, 20,
                PeriodUnit.YEARS, "0"));
        assertThat(longTerm.series()).hasSize(21);
        assertThat(longTerm.series().get(longTerm.series().size() - 1).balance())
                .isEqualByComparingTo(longTerm.finalAmount());
    }

    @Test
    void rejectsPeriodsAboveLimit() {
        assertThatThrownBy(() -> InterestCalculator.calculate(in(InterestType.COMPOUND, "100", "1", RateUnit.MONTHLY,
                51, PeriodUnit.YEARS, "0"))).isInstanceOf(IllegalArgumentException.class);
    }
}

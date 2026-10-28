package br.finedu.calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo de juros simples e compostos, mês a mês, em BigDecimal (precisão DECIMAL128; os
 * valores exibidos são arredondados para centavos com HALF_EVEN).
 *
 * <p>Premissas:
 * <ul>
 *   <li>O período é convertido para meses; os aportes ocorrem ao FINAL de cada mês.</li>
 *   <li>Juros compostos com taxa anual usam a taxa mensal EQUIVALENTE: (1+a)^(1/12) − 1.</li>
 *   <li>Juros simples com taxa anual usam a taxa mensal PROPORCIONAL: a / 12.</li>
 *   <li>Juros simples incidem apenas sobre cada valor depositado, pelo tempo em que ficou aplicado.</li>
 *   <li>Não considera impostos, taxas, inflação ou variação da taxa: é uma simulação matemática,
 *       não uma previsão de rentabilidade.</li>
 * </ul>
 */
public final class InterestCalculator {

    public enum InterestType { SIMPLE, COMPOUND }

    public enum RateUnit { MONTHLY, YEARLY }

    public enum PeriodUnit { MONTHS, YEARS }

    public record Input(InterestType interestType, BigDecimal principal, BigDecimal ratePercent, RateUnit rateUnit,
                        int period, PeriodUnit periodUnit, BigDecimal monthlyContribution) {
    }

    public record Point(int month, BigDecimal invested, BigDecimal interest, BigDecimal balance) {
    }

    public record Result(BigDecimal finalAmount, BigDecimal totalInvested, BigDecimal totalInterest,
                         BigDecimal monthlyRatePercent, int months, List<Point> series) {
    }

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    public static final int MAX_MONTHS = 600;

    private InterestCalculator() {
    }

    public static int toMonths(int period, PeriodUnit unit) {
        return unit == PeriodUnit.YEARS ? period * 12 : period;
    }

    /** Taxa mensal (fração decimal) usada no cálculo. */
    public static BigDecimal monthlyRate(InterestType type, BigDecimal ratePercent, RateUnit unit) {
        BigDecimal r = ratePercent.divide(HUNDRED, MC);
        if (unit == RateUnit.MONTHLY) {
            return r;
        }
        if (type == InterestType.SIMPLE) {
            return r.divide(BigDecimal.valueOf(12), MC);
        }
        double equivalent = Math.pow(1 + r.doubleValue(), 1.0 / 12.0) - 1;
        return new BigDecimal(equivalent, MC);
    }

    public static Result calculate(Input in) {
        int months = toMonths(in.period(), in.periodUnit());
        if (months < 1 || months > MAX_MONTHS) {
            throw new IllegalArgumentException("Período deve estar entre 1 e " + MAX_MONTHS + " meses.");
        }
        BigDecimal i = monthlyRate(in.interestType(), in.ratePercent(), in.rateUnit());
        BigDecimal p = in.principal();
        BigDecimal pmt = in.monthlyContribution() == null ? BigDecimal.ZERO : in.monthlyContribution();
        boolean sample = months > 120;

        List<Point> series = new ArrayList<>();
        series.add(point(0, p, p));
        BigDecimal balance = p;
        BigDecimal invested = p;
        for (int k = 1; k <= months; k++) {
            invested = invested.add(pmt);
            if (in.interestType() == InterestType.COMPOUND) {
                balance = balance.multiply(BigDecimal.ONE.add(i), MC).add(pmt);
            } else {
                balance = simpleBalance(p, pmt, i, k);
            }
            if (!sample || k % 12 == 0 || k == months) {
                series.add(point(k, invested, balance));
            }
        }
        BigDecimal finalAmount = round(balance);
        BigDecimal totalInvested = round(invested);
        return new Result(finalAmount, totalInvested, finalAmount.subtract(totalInvested),
                i.multiply(HUNDRED).setScale(6, RoundingMode.HALF_EVEN), months, series);
    }

    /** Montante em juros simples após k meses: P(1 + i·k) + Σ_{j=1..k} PMT(1 + i·(k − j)). */
    static BigDecimal simpleBalance(BigDecimal p, BigDecimal pmt, BigDecimal i, int k) {
        BigDecimal principalPart = p.multiply(BigDecimal.ONE.add(i.multiply(BigDecimal.valueOf(k))), MC);
        // Σ (1 + i(k−j)) para j=1..k = k + i · k(k−1)/2
        BigDecimal factor = BigDecimal.valueOf(k)
                .add(i.multiply(BigDecimal.valueOf((long) k * (k - 1) / 2), MC));
        return principalPart.add(pmt.multiply(factor, MC));
    }

    /** Fórmula fechada de juros compostos (usada nos testes para validar a iteração). */
    static BigDecimal compoundClosedForm(BigDecimal p, BigDecimal pmt, BigDecimal i, int n) {
        BigDecimal growth = BigDecimal.ONE.add(i).pow(n, MC);
        BigDecimal fvPrincipal = p.multiply(growth, MC);
        if (i.signum() == 0) {
            return fvPrincipal.add(pmt.multiply(BigDecimal.valueOf(n)));
        }
        BigDecimal fvContrib = pmt.multiply(growth.subtract(BigDecimal.ONE).divide(i, MC), MC);
        return fvPrincipal.add(fvContrib);
    }

    private static Point point(int month, BigDecimal invested, BigDecimal balance) {
        BigDecimal b = round(balance);
        BigDecimal inv = round(invested);
        return new Point(month, inv, b.subtract(inv), b);
    }

    private static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}

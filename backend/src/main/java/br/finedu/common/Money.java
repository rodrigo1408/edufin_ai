package br.finedu.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras centralizadas para valores monetários (sempre BigDecimal, 2 casas, HALF_EVEN). */
public final class Money {

    public static final BigDecimal MAX = new BigDecimal("999999999.99");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? zero() : of(value);
    }

    /** Valida valor estritamente positivo, com no máximo 2 casas decimais e dentro do limite. */
    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw ApiException.business("INVALID_AMOUNT", field + " deve ser maior que zero.");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw ApiException.business("INVALID_AMOUNT", field + " deve ter no máximo 2 casas decimais.");
        }
        if (value.compareTo(MAX) > 0) {
            throw ApiException.business("INVALID_AMOUNT", field + " excede o limite permitido.");
        }
        return of(value);
    }

    /**
     * Percentual de progresso (0-100, 1 casa decimal). Alvo zero/nulo resulta em 0 e saldo acima do
     * alvo é limitado a 100.
     */
    public static BigDecimal progressPercent(BigDecimal current, BigDecimal target) {
        if (target == null || target.signum() <= 0 || current == null || current.signum() <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_EVEN);
        }
        BigDecimal pct = current.multiply(HUNDRED).divide(target, 1, RoundingMode.HALF_EVEN);
        return pct.compareTo(HUNDRED) > 0 ? new BigDecimal("100.0") : pct;
    }
}

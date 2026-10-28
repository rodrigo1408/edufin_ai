package br.finedu.common;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Relógio da aplicação. Instantes são persistidos em UTC; regras baseadas em "dia" e "semana"
 * (sequências, desafios, limites diários de pontos) usam o fuso configurado (padrão: São Paulo).
 */
@Component
public class AppTime {

    private final Clock clock;
    private final ZoneId zone;

    public AppTime(Clock clock, @Value("${app.zone:America/Sao_Paulo}") String zone) {
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    public Instant now() {
        return Instant.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDate toDate(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    public LocalDate startOfWeek() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Identificador da semana ISO atual, ex.: 2026-W44. */
    public String isoWeekKey() {
        LocalDate t = today();
        return "%d-W%02d".formatted(t.get(IsoFields.WEEK_BASED_YEAR), t.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}

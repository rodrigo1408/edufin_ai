package br.finedu.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-28T12:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void blocksAfterLimitAndReleasesAfterWindow() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock, true);
        for (int i = 0; i < 3; i++) {
            limiter.check("k", 3, Duration.ofMinutes(1));
        }
        assertThatThrownBy(() -> limiter.check("k", 3, Duration.ofMinutes(1)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("RATE_LIMITED");
        assertThatCode(() -> limiter.check("other", 3, Duration.ofMinutes(1))).doesNotThrowAnyException();
        clock.now = clock.now.plusSeconds(61);
        assertThatCode(() -> limiter.check("k", 3, Duration.ofMinutes(1))).doesNotThrowAnyException();
    }

    @Test
    void canBeDisabled() {
        RateLimiter limiter = new RateLimiter(new MutableClock(), false);
        for (int i = 0; i < 100; i++) {
            limiter.check("k", 1, Duration.ofMinutes(1));
        }
    }
}

package br.finedu.common;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Limitador de requisições por janela deslizante, em memória. Suficiente para uma única
 * instância (arquitetura monolítica adotada). Ao escalar horizontalmente, substituir por uma
 * implementação compartilhada.
 */
@Component
public class RateLimiter {

    private static final int MAX_KEYS = 50_000;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final Clock clock;
    private final boolean enabled;

    public RateLimiter(Clock clock, @Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this.clock = clock;
        this.enabled = enabled;
    }

    /** Lança 429 se a chave exceder {@code limit} chamadas dentro de {@code window}. */
    public void check(String key, int limit, Duration window) {
        if (!enabled) {
            return;
        }
        long now = clock.millis();
        long from = now - window.toMillis();
        if (hits.size() > MAX_KEYS) {
            hits.entrySet().removeIf(e -> {
                Long last = e.getValue().peekLast();
                return last == null || last < from;
            });
        }
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < from) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Muitas tentativas em pouco tempo. Aguarde um pouco e tente novamente.");
            }
            deque.addLast(now);
        }
    }

    public void reset() {
        hits.clear();
    }
}

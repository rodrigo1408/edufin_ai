package br.finedu.user;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityLogRepository repository;
    private final Clock clock;

    public ActivityService(ActivityLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Participa da transação do caso de uso para que o histórico só exista se a operação for confirmada. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long userId, ActivityLog.Type type, String description) {
        repository.save(new ActivityLog(userId, type, description, Instant.now(clock)));
    }
}

package br.finedu.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "point_events")
public class PointEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PointRule eventType;

    @Column(name = "reference_key", nullable = false, length = 60)
    private String referenceKey;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PointEvent() {
    }

    public PointEvent(Long userId, PointRule eventType, String referenceKey, int points, String description,
                      Instant createdAt) {
        this.userId = userId;
        this.eventType = eventType;
        this.referenceKey = referenceKey;
        this.points = points;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public PointRule getEventType() {
        return eventType;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public int getPoints() {
        return points;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

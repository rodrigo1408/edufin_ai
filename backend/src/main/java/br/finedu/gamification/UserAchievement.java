package br.finedu.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    protected UserAchievement() {
    }

    public UserAchievement(Long userId, Achievement achievement, Instant unlockedAt) {
        this.userId = userId;
        this.achievement = achievement;
        this.unlockedAt = unlockedAt;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}

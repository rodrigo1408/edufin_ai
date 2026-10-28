package br.finedu.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    public enum Theme { SYSTEM, LIGHT, DARK }

    @Id
    @Column(name = "user_id")
    private Long userId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Theme theme = Theme.SYSTEM;

    @Column(name = "notify_goals", nullable = false)
    private boolean notifyGoals = true;

    @Column(name = "notify_learning", nullable = false)
    private boolean notifyLearning = true;

    @Column(name = "notify_achievements", nullable = false)
    private boolean notifyAchievements = true;

    @Column(name = "notify_community", nullable = false)
    private boolean notifyCommunity = true;

    @Column(name = "weekly_lesson_target", nullable = false)
    private int weeklyLessonTarget = 3;

    protected UserPreferences() {
    }

    public UserPreferences(Long userId) {
        this.userId = userId;
    }

    public void update(Theme theme, boolean notifyGoals, boolean notifyLearning, boolean notifyAchievements,
                       boolean notifyCommunity, int weeklyLessonTarget) {
        this.theme = theme;
        this.notifyGoals = notifyGoals;
        this.notifyLearning = notifyLearning;
        this.notifyAchievements = notifyAchievements;
        this.notifyCommunity = notifyCommunity;
        this.weeklyLessonTarget = weeklyLessonTarget;
    }

    public Long getUserId() {
        return userId;
    }

    public Theme getTheme() {
        return theme;
    }

    public boolean isNotifyGoals() {
        return notifyGoals;
    }

    public boolean isNotifyLearning() {
        return notifyLearning;
    }

    public boolean isNotifyAchievements() {
        return notifyAchievements;
    }

    public boolean isNotifyCommunity() {
        return notifyCommunity;
    }

    public int getWeeklyLessonTarget() {
        return weeklyLessonTarget;
    }
}

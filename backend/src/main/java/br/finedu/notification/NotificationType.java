package br.finedu.notification;

/** Tipo da notificação e a categoria de preferência que o controla. */
public enum NotificationType {
    WELCOME(Category.SYSTEM),
    GOAL_COMPLETED(Category.GOALS),
    GOAL_DEADLINE(Category.GOALS),
    GOAL_OVERDUE(Category.GOALS),
    LEARNING_REMINDER(Category.LEARNING),
    COURSE_COMPLETED(Category.LEARNING),
    ACHIEVEMENT_UNLOCKED(Category.ACHIEVEMENTS),
    CHALLENGE_COMPLETED(Category.ACHIEVEMENTS),
    POST_COMMENTED(Category.COMMUNITY),
    CONTENT_MODERATED(Category.SYSTEM),
    FEEDBACK_ANSWERED(Category.SYSTEM);

    public enum Category { SYSTEM, GOALS, LEARNING, ACHIEVEMENTS, COMMUNITY }

    private final Category category;

    NotificationType(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }
}

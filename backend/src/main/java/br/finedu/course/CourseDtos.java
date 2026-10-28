package br.finedu.course;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import br.finedu.gamification.GamificationService.Reward;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CourseDtos {

    private CourseDtos() {
    }

    public record CourseCard(Long id, String slug, String title, String summary, Topic category, String categoryLabel,
                             Difficulty difficulty, int estimatedMinutes, int position, int lessonCount,
                             BigDecimal averageRating, long reviewCount, boolean enrolled, boolean completed,
                             int completedLessons, BigDecimal progressPercent) {
    }

    public record LessonSummary(Long id, String title, int estimatedMinutes, boolean completed, boolean hasQuiz) {
    }

    public record ModuleView(Long id, String title, List<LessonSummary> lessons) {
    }

    public record EnrollmentView(Instant enrolledAt, Instant completedAt, Long lastLessonId, Long nextLessonId) {
    }

    public record ReviewView(Long id, String author, int rating, String comment, Instant createdAt) {
    }

    public record MyReview(int rating, String comment) {
    }

    public record CourseDetail(CourseCard course, String description, List<String> objectives,
                               List<ModuleView> modules, EnrollmentView enrollment, MyReview myReview) {
    }

    public record LessonView(Long id, String title, String content, int estimatedMinutes, Long courseId,
                             String courseTitle, String moduleTitle, boolean completed, String note,
                             Long quizId, Long previousLessonId, Long nextLessonId, int position, int totalLessons) {
    }

    public record CompletionResult(boolean courseCompleted, BigDecimal progressPercent, Long nextLessonId,
                                   Reward reward) {
    }

    public record NoteRequest(@NotNull @Size(max = 5000) String content) {
    }

    public record NoteView(Long lessonId, String lessonTitle, Long courseId, String courseTitle, String content,
                           Instant updatedAt) {
    }

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    public record LearningItem(Long courseId, String title, Topic category, BigDecimal progressPercent,
                               int completedLessons, int lessonCount, Long nextLessonId, Instant enrolledAt,
                               Instant completedAt) {
    }
}

package br.finedu.quiz;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import br.finedu.course.CourseDtos.CourseCard;
import br.finedu.gamification.GamificationService.Reward;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class QuizDtos {

    private QuizDtos() {
    }

    public record QuizCard(Long id, String title, String description, Topic category, String categoryLabel,
                           Difficulty difficulty, int questionCount, BigDecimal bestPercent, long attempts) {
    }

    /** Alternativa enviada durante o quiz: sem indicação de correção. */
    public record OptionView(Long id, String text) {
    }

    public record QuestionView(Long id, int position, String statement, List<OptionView> options) {
    }

    public record AnswerFeedback(Long questionId, Long selectedOptionId, boolean correct, Long correctOptionId,
                                 String explanation) {
    }

    public record AttemptView(Long attemptId, Long quizId, String quizTitle, QuizAttempt.Status status,
                              List<QuestionView> questions, List<AnswerFeedback> answered) {
    }

    public record AnswerRequest(@NotNull Long questionId, @NotNull Long optionId) {
    }

    public record AttemptResult(Long attemptId, Long quizId, String quizTitle, int correct, int total, int score,
                                BigDecimal percent, boolean passed, Instant finishedAt, List<AnswerFeedback> review,
                                List<String> weakTopics, List<CourseCard> recommendedCourses, Reward reward) {
    }

    public record AttemptHistoryItem(Long attemptId, Long quizId, String quizTitle, Topic category, int correct,
                                     int total, BigDecimal percent, Instant finishedAt) {
    }

    public record QuizDetail(QuizCard quiz, List<AttemptHistoryItem> myAttempts, Long inProgressAttemptId) {
    }
}

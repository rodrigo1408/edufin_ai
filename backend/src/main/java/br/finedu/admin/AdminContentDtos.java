package br.finedu.admin;

import br.finedu.common.Difficulty;
import br.finedu.common.Topic;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AdminContentDtos {

    private AdminContentDtos() {
    }

    public record CourseRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Use letras minúsculas, números e hífens.")
            @Size(max = 80) String slug,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 300) String summary,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 2000) String objectives,
            @NotNull Topic category,
            @NotNull Difficulty difficulty,
            @Min(1) @Max(1000) int estimatedMinutes,
            @Min(0) @Max(1000) int position,
            boolean published) {
    }

    public record ModuleRequest(@NotBlank @Size(max = 120) String title) {
    }

    public record LessonRequest(@NotBlank @Size(max = 120) String title,
                                @NotBlank @Size(max = 20000) String content,
                                @Min(1) @Max(240) int estimatedMinutes,
                                Long quizId) {
    }

    public record LessonAdminView(Long id, int position, String title, String content, int estimatedMinutes,
                                  Long quizId) {
    }

    public record ModuleAdminView(Long id, int position, String title, List<LessonAdminView> lessons) {
    }

    public record CourseAdminView(Long id, String slug, String title, String summary, String description,
                                  String objectives, Topic category, Difficulty difficulty, int estimatedMinutes,
                                  int position, boolean published, long enrollments, List<ModuleAdminView> modules) {
    }

    public record OptionRequest(@NotBlank @Size(max = 300) String text, boolean correct) {
    }

    public record QuestionRequest(@NotBlank @Size(max = 500) String statement,
                                  @NotBlank @Size(max = 1000) String explanation,
                                  @NotNull Topic topic,
                                  @NotNull @Size(min = 2, max = 6) List<@Valid @NotNull OptionRequest> options) {
    }

    public record QuizMetaRequest(@NotBlank @Size(max = 120) String title,
                                  @NotBlank @Size(max = 500) String description,
                                  @NotNull Topic category,
                                  @NotNull Difficulty difficulty,
                                  boolean published) {
    }

    public record QuizCreateRequest(@NotNull @Valid QuizMetaRequest quiz,
                                    @NotNull @Size(min = 1, max = 50) List<@Valid @NotNull QuestionRequest> questions) {
    }

    public record OptionAdminView(Long id, String text, boolean correct) {
    }

    public record QuestionAdminView(Long id, int position, String statement, String explanation, Topic topic,
                                    List<OptionAdminView> options) {
    }

    public record QuizAdminView(Long id, String title, String description, Topic category, Difficulty difficulty,
                                boolean published, boolean hasAttempts, List<QuestionAdminView> questions) {
    }
}

package br.finedu.common;

import br.finedu.course.CourseRepository;
import br.finedu.user.PasswordResetService;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informações públicas (sem login): recursos disponíveis e a trilha de cursos publicada. */
@RestController
public class PublicController {

    /** Resumo público de um curso, sem conteúdo das aulas nem dados de usuários. */
    public record PublicCourse(Long id, String title, String summary, Topic category, String categoryLabel,
                               Difficulty difficulty, int estimatedMinutes, int lessonCount) {
    }

    private final PasswordResetService passwordReset;
    private final CourseRepository courses;

    public PublicController(PasswordResetService passwordReset, CourseRepository courses) {
        this.passwordReset = passwordReset;
        this.courses = courses;
    }

    @GetMapping("/api/public/config")
    public Map<String, Object> config() {
        return Map.of(
                "passwordResetByEmail", passwordReset.isAvailable(),
                "externalNotifications", false,
                "chatbotProvider", "LOCAL_KNOWLEDGE_BASE",
                "generativeAi", false,
                "marketData", false);
    }

    @GetMapping("/api/public/courses")
    @Transactional(readOnly = true)
    public List<PublicCourse> courses() {
        return courses.findPublished(null, null).stream()
                .map(c -> new PublicCourse(c.getId(), c.getTitle(), c.getSummary(), c.getCategory(),
                        c.getCategory().label(), c.getDifficulty(), c.getEstimatedMinutes(), c.orderedLessons().size()))
                .toList();
    }
}

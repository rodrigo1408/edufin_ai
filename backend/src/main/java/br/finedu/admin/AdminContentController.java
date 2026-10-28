package br.finedu.admin;

import br.finedu.admin.AdminContentDtos.CourseAdminView;
import br.finedu.admin.AdminContentDtos.CourseRequest;
import br.finedu.admin.AdminContentDtos.LessonRequest;
import br.finedu.admin.AdminContentDtos.ModuleRequest;
import br.finedu.admin.AdminContentDtos.QuestionRequest;
import br.finedu.admin.AdminContentDtos.QuizAdminView;
import br.finedu.admin.AdminContentDtos.QuizCreateRequest;
import br.finedu.admin.AdminContentDtos.QuizMetaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /api/admin/content/** exige a permissão CONTENT_MANAGE (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    public record QuestionsRequest(@NotNull @Size(min = 1, max = 50) List<@Valid @NotNull QuestionRequest> questions) {
    }

    private final AdminContentService service;

    public AdminContentController(AdminContentService service) {
        this.service = service;
    }

    @GetMapping("/courses")
    public List<CourseAdminView> courses() {
        return service.listCourses();
    }

    @GetMapping("/courses/{id}")
    public CourseAdminView course(@PathVariable Long id) {
        return service.getCourse(id);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseAdminView createCourse(@Valid @RequestBody CourseRequest req) {
        return service.createCourse(req);
    }

    @PutMapping("/courses/{id}")
    public CourseAdminView updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        return service.updateCourse(id, req);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        service.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/courses/{id}/modules")
    public CourseAdminView addModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest req) {
        return service.addModule(id, req.title());
    }

    @PutMapping("/modules/{id}")
    public CourseAdminView renameModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest req) {
        return service.renameModule(id, req.title());
    }

    @DeleteMapping("/modules/{id}")
    public CourseAdminView deleteModule(@PathVariable Long id) {
        return service.deleteModule(id);
    }

    @PostMapping("/modules/{id}/lessons")
    public CourseAdminView addLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest req) {
        return service.addLesson(id, req);
    }

    @PutMapping("/lessons/{id}")
    public CourseAdminView updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest req) {
        return service.updateLesson(id, req);
    }

    @DeleteMapping("/lessons/{id}")
    public CourseAdminView deleteLesson(@PathVariable Long id) {
        return service.deleteLesson(id);
    }

    @GetMapping("/quizzes")
    public List<QuizAdminView> quizzes() {
        return service.listQuizzes();
    }

    @GetMapping("/quizzes/{id}")
    public QuizAdminView quiz(@PathVariable Long id) {
        return service.getQuiz(id);
    }

    @PostMapping("/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizAdminView createQuiz(@Valid @RequestBody QuizCreateRequest req) {
        return service.createQuiz(req);
    }

    @PutMapping("/quizzes/{id}")
    public QuizAdminView updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizMetaRequest req) {
        return service.updateQuizMeta(id, req);
    }

    @PutMapping("/quizzes/{id}/questions")
    public QuizAdminView replaceQuestions(@PathVariable Long id, @Valid @RequestBody QuestionsRequest req) {
        return service.replaceQuestions(id, req.questions());
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        service.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }
}

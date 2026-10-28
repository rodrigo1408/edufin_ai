package br.finedu.course;

import br.finedu.common.Difficulty;
import br.finedu.common.PageResponse;
import br.finedu.common.Topic;
import br.finedu.course.CourseDtos.CompletionResult;
import br.finedu.course.CourseDtos.CourseCard;
import br.finedu.course.CourseDtos.CourseDetail;
import br.finedu.course.CourseDtos.LearningItem;
import br.finedu.course.CourseDtos.LessonView;
import br.finedu.course.CourseDtos.MyReview;
import br.finedu.course.CourseDtos.NoteRequest;
import br.finedu.course.CourseDtos.NoteView;
import br.finedu.course.CourseDtos.ReviewRequest;
import br.finedu.course.CourseDtos.ReviewView;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping("/courses")
    public List<CourseCard> catalog(@AuthenticationPrincipal AuthUser user,
                                    @RequestParam(required = false) Topic topic,
                                    @RequestParam(required = false) Difficulty difficulty) {
        return service.catalog(user.id(), topic, difficulty);
    }

    @GetMapping("/courses/mine")
    public List<LearningItem> mine(@AuthenticationPrincipal AuthUser user) {
        return service.myLearning(user.id());
    }

    @GetMapping("/courses/notes")
    public List<NoteView> notes(@AuthenticationPrincipal AuthUser user) {
        return service.myNotes(user.id());
    }

    @GetMapping("/courses/{id}")
    public CourseDetail detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.detail(user.id(), id);
    }

    @PostMapping("/courses/{id}/enroll")
    public CourseDetail enroll(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.enroll(user.id(), id);
    }

    @GetMapping("/courses/{id}/reviews")
    public PageResponse<ReviewView> reviews(@PathVariable Long id,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.reviews(id, page, size);
    }

    @PutMapping("/courses/{id}/review")
    public MyReview review(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                           @Valid @RequestBody ReviewRequest req) {
        return service.review(user.id(), id, req);
    }

    @GetMapping("/lessons/{id}")
    public LessonView lesson(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.lesson(user.id(), id);
    }

    @PostMapping("/lessons/{id}/visit")
    public ResponseEntity<Void> visit(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.visit(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lessons/{id}/complete")
    public CompletionResult complete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.completeLesson(user.id(), id);
    }

    @PutMapping("/lessons/{id}/note")
    public NoteView note(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                         @Valid @RequestBody NoteRequest req) {
        return service.saveNote(user.id(), id, req.content());
    }
}

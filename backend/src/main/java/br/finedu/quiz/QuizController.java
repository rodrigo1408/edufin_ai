package br.finedu.quiz;

import br.finedu.common.Difficulty;
import br.finedu.common.PageResponse;
import br.finedu.common.Topic;
import br.finedu.quiz.QuizDtos.AnswerFeedback;
import br.finedu.quiz.QuizDtos.AnswerRequest;
import br.finedu.quiz.QuizDtos.AttemptHistoryItem;
import br.finedu.quiz.QuizDtos.AttemptResult;
import br.finedu.quiz.QuizDtos.AttemptView;
import br.finedu.quiz.QuizDtos.QuizCard;
import br.finedu.quiz.QuizDtos.QuizDetail;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    @GetMapping("/quizzes")
    public List<QuizCard> list(@AuthenticationPrincipal AuthUser user, @RequestParam(required = false) Topic topic,
                               @RequestParam(required = false) Difficulty difficulty) {
        return service.list(user.id(), topic, difficulty);
    }

    @GetMapping("/quizzes/{id}")
    public QuizDetail detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.detail(user.id(), id);
    }

    @PostMapping("/quizzes/{id}/attempts")
    public AttemptView start(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.start(user.id(), id);
    }

    @GetMapping("/quiz-attempts/history")
    public PageResponse<AttemptHistoryItem> history(@AuthenticationPrincipal AuthUser user,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(user.id(), page, size);
    }

    @GetMapping("/quiz-attempts/{id}")
    public AttemptView attempt(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.getAttempt(user.id(), id);
    }

    @PostMapping("/quiz-attempts/{id}/answers")
    public AnswerFeedback answer(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                 @Valid @RequestBody AnswerRequest req) {
        return service.answer(user.id(), id, req);
    }

    @PostMapping("/quiz-attempts/{id}/finish")
    public AttemptResult finish(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.finish(user.id(), id);
    }

    @GetMapping("/quiz-attempts/{id}/result")
    public AttemptResult result(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.result(user.id(), id);
    }
}

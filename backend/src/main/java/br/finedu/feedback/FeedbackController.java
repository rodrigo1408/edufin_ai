package br.finedu.feedback;

import br.finedu.common.PageResponse;
import br.finedu.feedback.FeedbackService.FeedbackRequest;
import br.finedu.feedback.FeedbackService.FeedbackView;
import br.finedu.feedback.FeedbackService.HandleRequest;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PostMapping("/api/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackView send(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody FeedbackRequest req) {
        return service.send(user.id(), req);
    }

    @GetMapping("/api/feedback")
    public List<FeedbackView> mine(@AuthenticationPrincipal AuthUser user) {
        return service.mine(user.id());
    }

    /** /api/admin/feedback/** exige FEEDBACK_MANAGE (configurado no SecurityConfig). */
    @GetMapping("/api/admin/feedback")
    public PageResponse<FeedbackView> search(@RequestParam(required = false) Feedback.Status status,
                                             @RequestParam(required = false) Feedback.Type type,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(status, type, page, size);
    }

    @PutMapping("/api/admin/feedback/{id}")
    public FeedbackView handle(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @Valid @RequestBody HandleRequest req) {
        return service.handle(user.id(), id, req);
    }
}

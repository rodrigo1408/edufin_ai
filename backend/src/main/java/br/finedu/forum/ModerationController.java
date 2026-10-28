package br.finedu.forum;

import br.finedu.common.PageResponse;
import br.finedu.forum.ForumDtos.ModeratedItem;
import br.finedu.forum.ForumDtos.ModerationRequest;
import br.finedu.forum.ForumDtos.ReportView;
import br.finedu.forum.ForumEntities.ReportStatus;
import br.finedu.forum.ForumEntities.Status;
import br.finedu.forum.ModerationService.ReviewModerationView;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Protegido por URL: /api/admin/moderation/** exige a permissão FORUM_MODERATE. */
@RestController
@RequestMapping("/api/admin/moderation")
public class ModerationController {

    private final ModerationService service;

    public ModerationController(ModerationService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public PageResponse<ReportView> reports(@RequestParam(defaultValue = "OPEN") ReportStatus status,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.reports(status, page, size);
    }

    @PostMapping("/reports/{id}/resolve")
    public ReportView resolve(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                              @Valid @RequestBody ModerationRequest req) {
        return service.resolve(user.id(), id, req.action());
    }

    @GetMapping("/posts")
    public PageResponse<ModeratedItem> posts(@RequestParam(defaultValue = "HIDDEN") Status status,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.moderated(status, page, size);
    }

    @GetMapping("/comments")
    public PageResponse<ModeratedItem> comments(@RequestParam(defaultValue = "HIDDEN") Status status,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.moderatedComments(status, page, size);
    }

    @PostMapping("/posts/{id}")
    public ResponseEntity<Void> moderatePost(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                             @Valid @RequestBody ModerationRequest req) {
        service.moderatePost(user.id(), id, req.action());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{id}")
    public ResponseEntity<Void> moderateComment(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                @Valid @RequestBody ModerationRequest req) {
        service.moderateComment(user.id(), id, req.action());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews")
    public PageResponse<ReviewModerationView> reviews(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.courseReviews(page, size);
    }

    @PostMapping("/reviews/{id}")
    public ResponseEntity<Void> setReviewHidden(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        service.setReviewHidden(id, Boolean.TRUE.equals(body.get("hidden")));
        return ResponseEntity.noContent().build();
    }
}

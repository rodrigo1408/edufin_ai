package br.finedu.forum;

import br.finedu.common.PageResponse;
import br.finedu.forum.ForumDtos.CommentRequest;
import br.finedu.forum.ForumDtos.CommentView;
import br.finedu.forum.ForumDtos.LikeResult;
import br.finedu.forum.ForumDtos.PostDetail;
import br.finedu.forum.ForumDtos.PostRequest;
import br.finedu.forum.ForumDtos.PostSummary;
import br.finedu.forum.ForumDtos.ReportRequest;
import br.finedu.forum.ForumEntities.Category;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService service;

    public ForumController(ForumService service) {
        this.service = service;
    }

    @GetMapping("/posts")
    public PageResponse<PostSummary> list(@AuthenticationPrincipal AuthUser user,
                                          @RequestParam(required = false) @Size(max = 60) String q,
                                          @RequestParam(required = false) Category category,
                                          @RequestParam(defaultValue = "false") boolean mine,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.list(user.id(), q, category, mine, page, size);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetail create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PostRequest req) {
        return service.create(user.id(), req);
    }

    @GetMapping("/posts/{id}")
    public PostDetail get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/posts/{id}")
    public PostDetail update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                             @Valid @RequestBody PostRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{id}/comments")
    public PageResponse<CommentView> comments(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.comments(user.id(), id, page, size);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView comment(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @Valid @RequestBody CommentRequest req) {
        return service.comment(user.id(), id, req);
    }

    @PutMapping("/comments/{id}")
    public CommentView editComment(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @Valid @RequestBody CommentRequest req) {
        return service.editComment(user.id(), id, req);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.deleteComment(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/like")
    public LikeResult like(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.like(user.id(), id);
    }

    @DeleteMapping("/posts/{id}/like")
    public LikeResult unlike(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.unlike(user.id(), id);
    }

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<Void> reportPost(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                           @Valid @RequestBody ReportRequest req) {
        service.reportPost(user.id(), id, req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/comments/{id}/report")
    public ResponseEntity<Void> reportComment(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                              @Valid @RequestBody ReportRequest req) {
        service.reportComment(user.id(), id, req);
        return ResponseEntity.accepted().build();
    }
}

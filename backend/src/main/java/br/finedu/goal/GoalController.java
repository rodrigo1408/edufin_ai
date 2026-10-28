package br.finedu.goal;

import br.finedu.common.PageResponse;
import br.finedu.goal.GoalDtos.GoalRequest;
import br.finedu.goal.GoalDtos.GoalResult;
import br.finedu.goal.GoalDtos.GoalView;
import br.finedu.goal.GoalDtos.MovementRequest;
import br.finedu.goal.GoalDtos.MovementResult;
import br.finedu.goal.GoalDtos.MovementView;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoalView> list(@AuthenticationPrincipal AuthUser user,
                               @RequestParam(defaultValue = "ALL") GoalService.Filter status) {
        return service.list(user.id(), status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResult create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody GoalRequest req) {
        return service.create(user.id(), req);
    }

    @GetMapping("/{id}")
    public GoalView get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public GoalView update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                           @Valid @RequestBody GoalRequest req) {
        return service.update(user.id(), id, req);
    }

    @PostMapping("/{id}/cancel")
    public GoalView cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.cancel(user.id(), id);
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResult addMovement(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                      @Valid @RequestBody MovementRequest req) {
        return service.addMovement(user.id(), id, req);
    }

    @GetMapping("/{id}/movements")
    public PageResponse<MovementView> movements(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.movements(user.id(), id, page, size);
    }
}

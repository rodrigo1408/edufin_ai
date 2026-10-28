package br.finedu.gamification;

import br.finedu.common.PageResponse;
import br.finedu.gamification.GamificationService.PointEventView;
import br.finedu.gamification.GamificationService.Summary;
import br.finedu.security.AuthUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura: pontos e conquistas são concedidos pelo servidor a partir das ações reais. */
@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationService service;

    public GamificationController(GamificationService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal AuthUser user) {
        return service.summary(user.id());
    }

    @GetMapping("/history")
    public PageResponse<PointEventView> history(@AuthenticationPrincipal AuthUser user,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(user.id(), page, size);
    }
}

package br.finedu.dashboard;

import br.finedu.dashboard.DashboardService.Dashboard;
import br.finedu.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal AuthUser user) {
        return service.build(user.id(), user.displayName());
    }
}

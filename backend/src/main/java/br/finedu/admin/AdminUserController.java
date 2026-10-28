package br.finedu.admin;

import br.finedu.admin.AdminUserService.AdminUserView;
import br.finedu.common.PageResponse;
import br.finedu.security.AuthUser;
import br.finedu.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** /api/admin/users/** exige a permissão USER_MANAGE (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record RoleRequest(@NotNull Role role) {
    }

    public record StatusRequest(@NotNull Boolean enabled) {
    }

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminUserView> search(@RequestParam(required = false) @Size(max = 60) String q,
                                              @RequestParam(required = false) Role role,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(q, role, page, size);
    }

    @PutMapping("/{id}/role")
    public AdminUserView role(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                              @Valid @RequestBody RoleRequest req) {
        return service.changeRole(admin.id(), id, req.role());
    }

    @PutMapping("/{id}/status")
    public AdminUserView status(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                @Valid @RequestBody StatusRequest req) {
        return service.setEnabled(admin.id(), id, req.enabled());
    }

    @PostMapping("/{id}/reset-link")
    public Map<String, String> resetLink(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return Map.of("resetUrl", service.resetLink(admin.id(), id),
                "notice", "Link de uso único, válido por 30 minutos. Entregue somente à própria pessoa.");
    }
}

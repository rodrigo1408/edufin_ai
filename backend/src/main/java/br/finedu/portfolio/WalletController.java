package br.finedu.portfolio;

import br.finedu.common.PageResponse;
import br.finedu.portfolio.WalletDtos.CreateResult;
import br.finedu.portfolio.WalletDtos.MovementRequest;
import br.finedu.portfolio.WalletDtos.MovementView;
import br.finedu.portfolio.WalletDtos.ReturnRequest;
import br.finedu.portfolio.WalletDtos.ScenarioRequest;
import br.finedu.portfolio.WalletDtos.ScenarioResponse;
import br.finedu.portfolio.WalletDtos.WalletDetail;
import br.finedu.portfolio.WalletDtos.WalletRequest;
import br.finedu.portfolio.WalletDtos.WalletSummary;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @GetMapping
    public List<WalletSummary> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResult create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody WalletRequest req) {
        return service.create(user.id(), req);
    }

    @GetMapping("/{id}")
    public WalletDetail detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.detail(user.id(), id);
    }

    @PutMapping("/{id}")
    public WalletDetail update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @Valid @RequestBody WalletRequest req) {
        return service.update(user.id(), id, req);
    }

    @PostMapping("/{id}/archive")
    public WalletDetail archive(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                @RequestParam(defaultValue = "true") boolean archived) {
        return service.setArchived(user.id(), id, archived);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/movements")
    public PageResponse<MovementView> movements(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.movements(user.id(), id, page, size);
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDetail addMovement(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                    @Valid @RequestBody MovementRequest req) {
        return service.addMovement(user.id(), id, req);
    }

    @PostMapping("/{id}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDetail applyReturn(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                    @Valid @RequestBody ReturnRequest req) {
        return service.applyMonthlyReturn(user.id(), id, req);
    }

    @PostMapping("/{id}/scenarios")
    public ScenarioResponse scenarios(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                      @Valid @RequestBody ScenarioRequest req) {
        return service.scenarios(user.id(), id, req);
    }
}

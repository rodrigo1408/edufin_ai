package br.finedu.calculator;

import br.finedu.calculator.CalculatorDtos.CalculationResponse;
import br.finedu.calculator.CalculatorDtos.CompareRequest;
import br.finedu.calculator.CalculatorDtos.InputDto;
import br.finedu.calculator.CalculatorDtos.SaveRequest;
import br.finedu.calculator.CalculatorDtos.SaveResponse;
import br.finedu.calculator.CalculatorDtos.SimulationView;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculator")
public class CalculatorController {

    private final CalculatorService service;

    public CalculatorController(CalculatorService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    public CalculationResponse calculate(@Valid @RequestBody InputDto input) {
        return service.calculate(input);
    }

    @PostMapping("/compare")
    public List<CalculationResponse> compare(@Valid @RequestBody CompareRequest req) {
        return req.scenarios().stream().map(service::calculate).toList();
    }

    @GetMapping("/simulations")
    public List<SimulationView> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.id());
    }

    @GetMapping("/simulations/{id}")
    public CalculationResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PostMapping("/simulations")
    @ResponseStatus(HttpStatus.CREATED)
    public SaveResponse save(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody SaveRequest req) {
        return service.save(user.id(), req);
    }

    @DeleteMapping("/simulations/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}

package br.finedu.transaction;

import br.finedu.common.PageResponse;
import br.finedu.security.AuthUser;
import br.finedu.transaction.TransactionDtos.CategoryOption;
import br.finedu.transaction.TransactionDtos.Summary;
import br.finedu.transaction.TransactionDtos.TransactionRequest;
import br.finedu.transaction.TransactionDtos.TransactionResult;
import br.finedu.transaction.TransactionDtos.TransactionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TransactionView> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Transaction.Type type,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) Transaction.Origin origin,
            @RequestParam(required = false) @Size(max = 60) String q,
            @RequestParam(defaultValue = "DATE") TransactionService.SortField sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(user.id(), new TransactionService.Filter(from, to, type, category, origin, q), sort,
                direction, page, size);
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal AuthUser user,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(user.id(), from, to);
    }

    @GetMapping("/categories")
    public List<CategoryOption> categories() {
        return Arrays.stream(TransactionCategory.values())
                .map(c -> new CategoryOption(c, c.label(), c.type())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResult create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody TransactionRequest req) {
        return service.create(user.id(), req);
    }

    @GetMapping("/{id}")
    public TransactionView get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public TransactionView update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                  @Valid @RequestBody TransactionRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}

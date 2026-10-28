package br.finedu.privacy;

import br.finedu.common.RateLimiter;
import br.finedu.security.AuthUser;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyController {

    private final DataExportService exportService;
    private final RateLimiter rateLimiter;

    public PrivacyController(DataExportService exportService, RateLimiter rateLimiter) {
        this.exportService = exportService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/api/me/export")
    public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal AuthUser user) {
        rateLimiter.check("export:" + user.id(), 5, Duration.ofHours(1));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"edufin-meus-dados.json\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(exportService.export(user.id()));
    }
}

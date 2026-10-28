package br.finedu.common;

import br.finedu.user.PasswordResetService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuração pública consumida pelo frontend para exibir apenas recursos realmente disponíveis. */
@RestController
public class PublicController {

    private final PasswordResetService passwordReset;

    public PublicController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    @GetMapping("/api/public/config")
    public Map<String, Object> config() {
        return Map.of(
                "passwordResetByEmail", passwordReset.isAvailable(),
                "externalNotifications", false,
                "chatbotProvider", "LOCAL_KNOWLEDGE_BASE",
                "marketData", false);
    }
}

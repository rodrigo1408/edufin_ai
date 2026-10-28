package br.finedu.chatbot;

import br.finedu.chatbot.ChatbotProvider.Mode;
import br.finedu.chatbot.ChatbotService.ChatResponse;
import br.finedu.chatbot.ChatbotService.Context;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    /**
     * @param message  pergunta livre (obrigatória no modo ASK)
     * @param mode     ASK (padrão), ALTERNATIVE ou EXAMPLE
     * @param entryId  resposta anterior a reformular/exemplificar
     * @param lessonId aula em estudo, para o tutor considerar o tema da aula
     */
    public record AskRequest(@Size(max = 500) String message, Mode mode, @Size(max = 40) String entryId,
                             Long lessonId) {
    }

    private final ChatbotService service;

    public ChatbotController(ChatbotService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AskRequest req) {
        return service.ask(user.id(), req.message(), req.mode(), req.entryId(), req.lessonId());
    }

    /** Dica conceitual para uma pergunta de quiz, sem revelar a alternativa correta. */
    @PostMapping("/hint/question/{questionId}")
    public ChatResponse hint(@AuthenticationPrincipal AuthUser user, @PathVariable Long questionId) {
        return service.hintForQuestion(user.id(), questionId);
    }

    @GetMapping("/info")
    public Map<String, Object> info(@RequestParam(required = false) Long lessonId) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("disclaimer", ChatbotService.DISCLAIMER);
        info.put("source", service.providerName());
        info.put("generativeModel", false);
        info.put("suggestions", service.suggestions(lessonId));
        Context context = service.lessonContext(lessonId);
        if (context != null) {
            info.put("context", context);
        }
        return info;
    }
}

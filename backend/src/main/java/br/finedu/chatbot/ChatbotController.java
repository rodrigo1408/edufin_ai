package br.finedu.chatbot;

import br.finedu.chatbot.ChatbotService.ChatResponse;
import br.finedu.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    public record AskRequest(@NotBlank @Size(max = 500) String message) {
    }

    private final ChatbotService service;
    private final LocalKnowledgeBaseProvider knowledgeBase;

    public ChatbotController(ChatbotService service, LocalKnowledgeBaseProvider knowledgeBase) {
        this.service = service;
        this.knowledgeBase = knowledgeBase;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AskRequest req) {
        return service.ask(user.id(), req.message());
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        List<String> samples = knowledgeBase.sampleQuestions();
        return Map.of("disclaimer", ChatbotService.DISCLAIMER, "source", knowledgeBase.name(), "suggestions", samples);
    }
}

package br.finedu.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import br.finedu.chatbot.ChatbotService.ChatResponse;
import br.finedu.common.RateLimiter;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ChatbotServiceTest {

    private ChatbotService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ChatbotService(new LocalKnowledgeBaseProvider(JsonMapper.builder().build()),
                new RateLimiter(Clock.systemUTC(), false));
    }

    @Test
    void answersFromKnowledgeBase() {
        ChatResponse r = service.ask(1L, "Como funcionam os juros compostos?");
        assertThat(r.matched()).isTrue();
        assertThat(r.topic()).isEqualTo("INTEREST");
        assertThat(r.answer()).contains("juros sobre juros");
        assertThat(r.source()).isEqualTo("LOCAL_KNOWLEDGE_BASE");
    }

    @Test
    void understandsQuestionsWithoutAccents() {
        assertThat(service.ask(1L, "o que e inflacao").topic()).isEqualTo("INFLATION");
        assertThat(service.ask(1L, "Como faço meu ORÇAMENTO?").topic()).isEqualTo("BUDGET");
    }

    @Test
    void refusesCredentialsAndCardNumbers() {
        ChatResponse r = service.ask(1L, "minha senha do banco é 1234, pode guardar?");
        assertThat(r.source()).isEqualTo("GUARDRAIL");
        assertThat(r.answer()).contains("não envie senhas");
        assertThat(service.ask(1L, "meu cartão 4111 1111 1111 1111").source()).isEqualTo("GUARDRAIL");
    }

    @Test
    void doesNotGivePersonalInvestmentAdviceOrPromises() {
        ChatResponse r = service.ask(1L, "Qual ação devo comprar que vai subir?");
        assertThat(r.source()).isEqualTo("GUARDRAIL");
        assertThat(r.answer()).contains("Não posso dizer em que você deve investir");
    }

    @Test
    void acknowledgesWhenItDoesNotKnow() {
        ChatResponse r = service.ask(1L, "qual a capital da Mongólia?");
        assertThat(r.matched()).isFalse();
        assertThat(r.answer()).contains("Ainda não sei responder");
        assertThat(r.suggestions()).isNotEmpty();
        assertThat(r.disclaimer()).contains("Não é consultoria financeira");
    }
}

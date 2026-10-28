package br.finedu.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.finedu.chatbot.ChatbotProvider.Mode;
import br.finedu.chatbot.ChatbotService.ChatResponse;
import br.finedu.common.ApiException;
import br.finedu.common.RateLimiter;
import br.finedu.course.LessonRepository;
import br.finedu.quiz.QuizQuestionRepository;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ChatbotServiceTest {

    private ChatbotService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ChatbotService(new LocalKnowledgeBaseProvider(JsonMapper.builder().build()),
                new RateLimiter(Clock.systemUTC(), false), mock(LessonRepository.class), mock(QuizQuestionRepository.class));
    }

    private ChatResponse ask(String q) {
        return service.ask(1L, q, Mode.ASK, null, null);
    }

    @Test
    void answersFromKnowledgeBase() {
        ChatResponse r = ask("Como funcionam os juros compostos?");
        assertThat(r.matched()).isTrue();
        assertThat(r.topic()).isEqualTo("INTEREST");
        assertThat(r.answer()).contains("juros sobre juros");
        assertThat(r.source()).isEqualTo("LOCAL_KNOWLEDGE_BASE");
        assertThat(r.entryId()).isEqualTo("interest-03");
        assertThat(r.canRephrase()).isTrue();
        assertThat(r.canGiveExample()).isTrue();
    }

    @Test
    void specificPhrasesWinOverGenericOnes() {
        assertThat(ask("o que são juros compostos?").entryId()).isEqualTo("interest-03");
        assertThat(ask("o que são juros?").entryId()).isEqualTo("interest-01");
    }

    @Test
    void understandsQuestionsWithoutAccents() {
        assertThat(ask("o que e inflacao").topic()).isEqualTo("INFLATION");
        assertThat(ask("Como faço meu ORÇAMENTO?").topic()).isEqualTo("BUDGET");
    }

    @Test
    void explainsAnotherWayAndGivesExamples() {
        String entry = ask("o que é inflação?").entryId();
        ChatResponse alt = service.ask(1L, null, Mode.ALTERNATIVE, entry, null);
        ChatResponse ex = service.ask(1L, null, Mode.EXAMPLE, entry, null);
        assertThat(alt.answer()).contains("encolhesse").isNotEqualTo(ask("o que é inflação?").answer());
        assertThat(ex.answer()).startsWith("Exemplo:");
        assertThat(alt.mode()).isEqualTo(Mode.ALTERNATIVE);
    }

    @Test
    void followUpRequiresAPreviousAnswer() {
        assertThatThrownBy(() -> service.ask(1L, null, Mode.EXAMPLE, null, null))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("MISSING_CONTEXT");
        assertThatThrownBy(() -> service.ask(1L, "  ", Mode.ASK, null, null))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("EMPTY_QUESTION");
    }

    @Test
    void refusesCredentialsAndCardNumbers() {
        ChatResponse r = ask("minha senha do banco é 1234, pode guardar?");
        assertThat(r.source()).isEqualTo("GUARDRAIL");
        assertThat(r.answer()).contains("não envie senhas");
        assertThat(ask("meu cartão 4111 1111 1111 1111").source()).isEqualTo("GUARDRAIL");
    }

    @Test
    void doesNotGivePersonalInvestmentAdviceOrPromises() {
        ChatResponse r = ask("Qual ação devo comprar que vai subir?");
        assertThat(r.source()).isEqualTo("GUARDRAIL");
        assertThat(r.answer()).contains("Não posso dizer em que você deve investir");
    }

    @Test
    void acknowledgesWhenItDoesNotKnow() {
        ChatResponse r = ask("qual a capital da Mongólia?");
        assertThat(r.matched()).isFalse();
        assertThat(r.answer()).contains("Ainda não sei responder");
        assertThat(r.suggestions()).isNotEmpty();
        assertThat(r.disclaimer()).contains("sem modelo de IA generativa");
    }
}

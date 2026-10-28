package br.finedu.chatbot;

import br.finedu.chatbot.ChatbotProvider.Link;
import br.finedu.chatbot.ChatbotProvider.Mode;
import br.finedu.chatbot.ChatbotProvider.ProviderAnswer;
import br.finedu.chatbot.ChatbotProvider.Query;
import br.finedu.common.ApiException;
import br.finedu.common.RateLimiter;
import br.finedu.common.Topic;
import br.finedu.course.Course;
import br.finedu.course.Lesson;
import br.finedu.course.LessonRepository;
import br.finedu.quiz.QuizQuestion;
import br.finedu.quiz.QuizQuestionRepository;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de atendimento do tutor, independentes da apresentação e do provedor:
 * <ol>
 *   <li>nunca processa nem pede senhas, números de cartão ou documentos;</li>
 *   <li>não dá recomendação personalizada de investimento nem promete rentabilidade;</li>
 *   <li>em exercícios, explica o conceito sem revelar a alternativa correta;</li>
 *   <li>não acessa dados financeiros de nenhum usuário (só conteúdo educativo);</li>
 *   <li>as perguntas não são armazenadas nem registradas em log.</li>
 * </ol>
 */
@Service
public class ChatbotService {

    public static final String DISCLAIMER = "Tutor educativo com explicações curadas pela equipe (sem modelo de IA "
            + "generativa conectado). Não é consultoria financeira e não substitui a orientação de um adulto de confiança.";

    /** Contexto atual do estudo, devolvido para a interface mostrar ao aluno. */
    public record Context(String kind, Long id, String title, Topic topic, String topicLabel) {
    }

    public record ChatResponse(String answer, String topic, boolean matched, List<Link> links, List<String> suggestions,
                               String source, String disclaimer, String entryId, Mode mode, boolean canRephrase,
                               boolean canGiveExample, Context context) {
    }

    private static final Pattern CARD_NUMBER = Pattern.compile("(\\d[ -]?){13,19}");
    private static final Pattern CREDENTIALS = Pattern.compile(
            "\\b(minha senha|senha (do|da|e)|meu cpf|cvv|codigo de seguranca|token do banco|numero do (meu )?cartao)\\b");
    private static final Pattern PERSONAL_ADVICE = Pattern.compile(
            "(qual acao (devo )?comprar|em que (eu )?devo investir|onde (eu )?devo investir|vai subir|vai cair|"
                    + "ficar rico rapido|dinheiro facil|garantid[oa]|melhor investimento pra mim|"
                    + "devo comprar (bitcoin|cripto|acoes)|qual cripto)");

    private final ChatbotProvider provider;
    private final RateLimiter rateLimiter;
    private final LessonRepository lessons;
    private final QuizQuestionRepository questions;

    public ChatbotService(ChatbotProvider provider, RateLimiter rateLimiter, LessonRepository lessons,
                          QuizQuestionRepository questions) {
        this.provider = provider;
        this.rateLimiter = rateLimiter;
        this.lessons = lessons;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public ChatResponse ask(Long userId, String question, Mode mode, String entryId, Long lessonId) {
        rateLimiter.check("chatbot:" + userId, 30, Duration.ofMinutes(1));
        Mode m = mode == null ? Mode.ASK : mode;
        Context context = lessonContext(lessonId);
        String topic = context == null ? null : context.topic().name();

        if (m == Mode.ASK) {
            if (question == null || question.isBlank()) {
                throw ApiException.business("EMPTY_QUESTION", "Escreva sua pergunta para o tutor.");
            }
            ChatResponse guarded = guardrails(question, context);
            if (guarded != null) {
                return guarded;
            }
        } else if (entryId == null || entryId.isBlank()) {
            throw ApiException.business("MISSING_CONTEXT", "Faça uma pergunta antes de pedir outra explicação ou um exemplo.");
        }
        ProviderAnswer a = provider.answer(new Query(question, m, entryId, topic));
        return response(a, m, context);
    }

    /**
     * Dica para um exercício: explica o conceito do tema da pergunta, sem indicar a alternativa
     * correta. A correção continua acontecendo só ao responder.
     */
    @Transactional(readOnly = true)
    public ChatResponse hintForQuestion(Long userId, Long questionId) {
        rateLimiter.check("chatbot:" + userId, 30, Duration.ofMinutes(1));
        QuizQuestion q = questions.findPublished(questionId).orElseThrow(() -> ApiException.notFound("Pergunta"));
        Topic topic = q.getTopic();
        // Explicação do mesmo tema mais próxima do enunciado; sem correspondência, a explicação base do tema.
        ProviderAnswer closest = provider.answer(new Query(q.getStatement(), Mode.ASK, null, topic.name()));
        ProviderAnswer concept = closest.matched() && topic.name().equals(closest.topic())
                ? closest : provider.conceptOf(topic.name());
        String text = "Vou te ajudar sem entregar a resposta. Esta pergunta é sobre " + topic.label().toLowerCase()
                + ". Relembre o conceito:\n\n" + concept.answer()
                + "\n\nAgora releia o enunciado e elimine as alternativas que contradizem essa ideia.";
        Context context = new Context("QUESTION", q.getId(), q.getQuiz().getTitle(), topic, topic.label());
        return new ChatResponse(text, topic.name(), concept.matched(), concept.links(), List.of(), provider.name(),
                DISCLAIMER, concept.entryId(), Mode.ASK, concept.hasAlternative(), concept.hasExample(), context);
    }

    @Transactional(readOnly = true)
    public List<String> suggestions(Long lessonId) {
        Context context = lessonContext(lessonId);
        return provider.suggestions(context == null ? null : context.topic().name(), 6);
    }

    @Transactional(readOnly = true)
    public Context lessonContext(Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        Lesson lesson = lessons.findWithCourse(lessonId).orElseThrow(() -> ApiException.notFound("Aula"));
        Course course = lesson.getModule().getCourse();
        if (!course.isPublished()) {
            throw ApiException.notFound("Aula");
        }
        return new Context("LESSON", lesson.getId(), lesson.getTitle(), course.getCategory(), course.getCategory().label());
    }

    public String providerName() {
        return provider.name();
    }

    private ChatResponse guardrails(String question, Context context) {
        String normalized = LocalKnowledgeBaseProvider.normalize(question);
        if (CARD_NUMBER.matcher(question).find() || CREDENTIALS.matcher(normalized).find()) {
            return new ChatResponse("Por segurança, não envie senhas, números de cartão, CPF ou códigos aqui — e nunca "
                    + "compartilhe esses dados com ninguém pela internet, nem com quem diz ser do banco. Eu não preciso "
                    + "dessas informações para ajudar. Se alguém pediu esses dados para você, converse com um adulto de "
                    + "confiança: pode ser um golpe.", "SAFETY", true,
                    List.of(new Link("Curso: Consumo consciente e segurança", "/cursos")), List.of(), "GUARDRAIL",
                    DISCLAIMER, null, Mode.ASK, false, false, context);
        }
        if (PERSONAL_ADVICE.matcher(normalized).find()) {
            return new ChatResponse("Não posso dizer em que você deve investir nem prever se algo vai subir ou cair — "
                    + "ninguém consegue garantir isso, e desconfie de quem promete ganhos garantidos. O que posso fazer é "
                    + "explicar conceitos: risco x retorno, reserva de emergência, renda fixa x renda variável e "
                    + "diversificação. Decisões reais de investimento devem ser tomadas com um responsável e, se "
                    + "necessário, com um profissional certificado.", "INVESTING", true,
                    List.of(new Link("Curso: Primeiros passos nos investimentos", "/cursos"),
                            new Link("Carteira simulada", "/carteiras")),
                    List.of("O que é risco e retorno?", "O que é renda fixa?", "O que é diversificação?"),
                    "GUARDRAIL", DISCLAIMER, null, Mode.ASK, false, false, context);
        }
        return null;
    }

    private ChatResponse response(ProviderAnswer a, Mode mode, Context context) {
        return new ChatResponse(a.answer(), a.topic(), a.matched(), a.links(), a.suggestions(), provider.name(),
                DISCLAIMER, a.entryId(), mode, a.hasAlternative(), a.hasExample(), context);
    }
}

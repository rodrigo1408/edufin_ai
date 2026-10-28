package br.finedu.chatbot;

import br.finedu.chatbot.ChatbotProvider.Link;
import br.finedu.chatbot.ChatbotProvider.ProviderAnswer;
import br.finedu.common.RateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Regras de atendimento do assistente, independentes da apresentação e do provedor:
 * <ol>
 *   <li>nunca processa nem pede senhas, números de cartão ou documentos;</li>
 *   <li>não dá recomendação personalizada de investimento nem promete rentabilidade;</li>
 *   <li>não acessa dados de nenhum usuário (só conteúdo educativo público);</li>
 *   <li>as perguntas não são armazenadas nem registradas em log.</li>
 * </ol>
 */
@Service
public class ChatbotService {

    public static final String DISCLAIMER = "Assistente educativo automático com respostas pré-definidas. "
            + "Não é consultoria financeira e não substitui a orientação de um adulto de confiança.";

    public record ChatResponse(String answer, String topic, boolean matched, List<Link> links,
                               List<String> suggestions, String source, String disclaimer) {
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

    public ChatbotService(ChatbotProvider provider, RateLimiter rateLimiter) {
        this.provider = provider;
        this.rateLimiter = rateLimiter;
    }

    public ChatResponse ask(Long userId, String question) {
        rateLimiter.check("chatbot:" + userId, 30, Duration.ofMinutes(1));
        String normalized = LocalKnowledgeBaseProvider.normalize(question);

        if (CARD_NUMBER.matcher(question).find() || CREDENTIALS.matcher(normalized).find()) {
            return new ChatResponse("Por segurança, não envie senhas, números de cartão, CPF ou códigos aqui — e nunca "
                    + "compartilhe esses dados com ninguém pela internet, nem com quem diz ser do banco. Eu não preciso "
                    + "dessas informações para ajudar. Se alguém pediu esses dados para você, converse com um adulto de "
                    + "confiança: pode ser um golpe.", "SAFETY", true,
                    List.of(new Link("Curso: Golpes e segurança digital", "/cursos")), List.of(), "GUARDRAIL", DISCLAIMER);
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
                    "GUARDRAIL", DISCLAIMER);
        }
        ProviderAnswer a = provider.answer(question);
        return new ChatResponse(a.answer(), a.topic(), a.matched(), a.links(), a.suggestions(), provider.name(),
                DISCLAIMER);
    }
}

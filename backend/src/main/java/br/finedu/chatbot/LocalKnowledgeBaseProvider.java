package br.finedu.chatbot;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Tutor local baseado em uma base de conhecimento curada (resources/chatbot/knowledge-base.json).
 * Faz correspondência por palavras-chave normalizadas (sem acentos) e prioriza o tema da aula ou do
 * exercício em andamento. Não gera texto novo: só devolve explicações revisadas (resposta principal,
 * explicação alternativa e exemplo), o que evita afirmações inventadas. Quando não encontra uma
 * resposta adequada, reconhece a limitação e sugere temas disponíveis.
 */
@Component
public class LocalKnowledgeBaseProvider implements ChatbotProvider {

    record Entry(String id, String topic, List<String> keywords, String question, String answer, String alternative,
                 String example, List<Link> links) {
    }

    private static final Set<String> STOPWORDS = Set.of("o", "a", "os", "as", "de", "do", "da", "dos", "das", "e",
            "um", "uma", "que", "eu", "me", "meu", "minha", "como", "para", "pra", "por", "no", "na", "em", "se",
            "com", "ou", "qual", "quais", "sobre", "isso", "tem", "ter", "ser", "sao", "esta", "estao", "voce", "vc", "posso",
            "funciona", "funcionam");
    private static final int MIN_SCORE = 2;
    private static final int TOPIC_BONUS = 1;

    private final List<Entry> entries;

    public LocalKnowledgeBaseProvider(ObjectMapper mapper) throws IOException {
        try (InputStream in = new ClassPathResource("chatbot/knowledge-base.json").getInputStream()) {
            this.entries = List.of(mapper.readValue(in, Entry[].class));
        }
    }

    @Override
    public String name() {
        return "LOCAL_KNOWLEDGE_BASE";
    }

    @Override
    public ProviderAnswer answer(Query q) {
        if (q.mode() != Mode.ASK) {
            return followUp(q);
        }
        String text = normalize(q.question());
        Set<String> tokens = tokens(text);
        Entry best = null;
        int bestScore = 0;
        for (Entry e : entries) {
            int score = score(e, text, tokens);
            if (score > 0 && q.topic() != null && q.topic().equals(e.topic())) {
                score += TOPIC_BONUS;
            }
            if (score > bestScore) {
                best = e;
                bestScore = score;
            }
        }
        if (best == null || bestScore < MIN_SCORE) {
            return new ProviderAnswer(false, null, null,
                    "Ainda não sei responder essa pergunta com segurança. Sou um tutor com explicações curadas sobre "
                            + "educação financeira e sobre a plataforma. Tente reformular, escolha um dos temas "
                            + "sugeridos ou procure nos cursos e na Comunidade.",
                    List.of(new Link("Ver cursos", "/cursos"), new Link("Comunidade", "/comunidade")),
                    suggestions(q.topic(), 4), false, false);
        }
        return toAnswer(best, best.answer());
    }

    private ProviderAnswer followUp(Query q) {
        Optional<Entry> entry = entries.stream().filter(e -> e.id().equals(q.entryId())).findFirst();
        if (entry.isEmpty()) {
            return new ProviderAnswer(false, null, null,
                    "Não encontrei a explicação anterior. Faça a pergunta novamente para eu explicar de outro jeito.",
                    List.of(), suggestions(q.topic(), 4), false, false);
        }
        Entry e = entry.get();
        String text = q.mode() == Mode.ALTERNATIVE ? e.alternative() : e.example();
        if (text == null) {
            return new ProviderAnswer(true, e.id(), e.topic(),
                    "Para este assunto eu só tenho a explicação principal. Veja os links indicados ou pergunte na Comunidade.",
                    links(e), suggestions(e.topic(), 3), false, false);
        }
        return toAnswer(e, text);
    }

    @Override
    public ProviderAnswer conceptOf(String topic) {
        return entries.stream().filter(e -> e.topic().equals(topic)).min(Comparator.comparing(Entry::id))
                .map(e -> toAnswer(e, e.answer()))
                .orElseGet(() -> new ProviderAnswer(false, null, topic,
                        "Ainda não tenho uma explicação para este tema. Revise a aula relacionada ao exercício.",
                        List.of(new Link("Ver cursos", "/cursos")), List.of(), false, false));
    }

    @Override
    public List<String> suggestions(String topic, int limit) {
        Stream<Entry> sameTopic = entries.stream().filter(e -> topic != null && e.topic().equals(topic));
        Stream<Entry> others = entries.stream().filter(e -> topic == null || !e.topic().equals(topic))
                .sorted(Comparator.comparing(Entry::id));
        return Stream.concat(sameTopic, others).map(Entry::question).distinct().limit(limit).toList();
    }

    public List<String> sampleQuestions() {
        return suggestions(null, 8);
    }

    private ProviderAnswer toAnswer(Entry e, String text) {
        List<String> related = entries.stream().filter(x -> x != e && x.topic().equals(e.topic()))
                .map(Entry::question).limit(3).toList();
        return new ProviderAnswer(true, e.id(), e.topic(), text, links(e),
                related.isEmpty() ? suggestions(null, 3) : related, e.alternative() != null, e.example() != null);
    }

    private static List<Link> links(Entry e) {
        return e.links() == null ? List.of() : e.links();
    }

    private int score(Entry e, String text, Set<String> tokens) {
        int score = 0;
        for (String raw : e.keywords()) {
            String k = normalize(raw);
            if (k.contains(" ")) {
                // Frases valem pelas palavras significativas: "o que sao juros" (1) < "juros compostos" (2).
                if (text.contains(k)) {
                    score += 1 + tokens(k).size();
                }
            } else if (tokens.contains(k) || tokens.stream().anyMatch(t -> t.length() > 4 && k.length() > 4
                    && (t.startsWith(k) || k.startsWith(t)))) {
                score += 2;
            }
        }
        return score;
    }

    static String normalize(String s) {
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("[^a-z0-9% ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static Set<String> tokens(String normalized) {
        Set<String> set = new HashSet<>(Arrays.asList(normalized.split(" ")));
        set.removeAll(STOPWORDS);
        set.remove("");
        return set;
    }
}

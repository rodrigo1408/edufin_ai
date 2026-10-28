package br.finedu.chatbot;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Assistente local baseado em uma base de conhecimento curada (resources/chatbot/knowledge-base.json).
 * Faz correspondência por palavras-chave normalizadas (sem acentos). Não gera texto novo: só
 * devolve respostas revisadas, o que evita afirmações inventadas. Quando não encontra uma resposta
 * adequada, reconhece a limitação e sugere temas disponíveis.
 */
@Component
public class LocalKnowledgeBaseProvider implements ChatbotProvider {

    record Entry(String id, String topic, List<String> keywords, String question, String answer, List<Link> links) {
    }

    private static final Set<String> STOPWORDS = Set.of("o", "a", "os", "as", "de", "do", "da", "dos", "das", "e",
            "um", "uma", "que", "eu", "me", "meu", "minha", "como", "para", "pra", "por", "no", "na", "em", "se",
            "com", "ou", "qual", "quais", "sobre", "isso", "tem", "ter", "ser", "voce", "vc", "posso", "funciona");
    private static final int MIN_SCORE = 2;

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
    public ProviderAnswer answer(String question) {
        String text = normalize(question);
        Set<String> tokens = tokens(text);
        Entry best = null;
        int bestScore = 0;
        for (Entry e : entries) {
            int score = score(e, text, tokens);
            if (score > bestScore) {
                best = e;
                bestScore = score;
            }
        }
        if (best == null || bestScore < MIN_SCORE) {
            return new ProviderAnswer(false, null,
                    "Ainda não sei responder essa pergunta com segurança. 😅 Sou um assistente com respostas "
                            + "pré-definidas sobre educação financeira e sobre a plataforma. Tente reformular ou escolha "
                            + "um dos temas sugeridos. Você também pode procurar nos cursos ou perguntar na Comunidade.",
                    List.of(new Link("Ver cursos", "/cursos"), new Link("Comunidade", "/comunidade")),
                    suggestions(null));
        }
        return new ProviderAnswer(true, best.topic(), best.answer(), best.links() == null ? List.of() : best.links(),
                suggestions(best));
    }

    public List<String> sampleQuestions() {
        return entries.stream().map(Entry::question).limit(8).toList();
    }

    private int score(Entry e, String text, Set<String> tokens) {
        int score = 0;
        for (String raw : e.keywords()) {
            String k = normalize(raw);
            if (k.contains(" ")) {
                if (text.contains(k)) {
                    score += 3;
                }
            } else if (tokens.contains(k) || tokens.stream().anyMatch(t -> t.length() > 4 && k.length() > 4
                    && (t.startsWith(k) || k.startsWith(t)))) {
                score += 2;
            }
        }
        return score;
    }

    private List<String> suggestions(Entry current) {
        List<Entry> pool = new ArrayList<>(entries);
        pool.sort(Comparator.comparing(Entry::id));
        List<String> result = new ArrayList<>();
        for (Entry e : pool) {
            if (current != null && (e == current || !e.topic().equals(current.topic()))) {
                continue;
            }
            result.add(e.question());
            if (result.size() == 3) {
                break;
            }
        }
        if (result.isEmpty() || current == null) {
            result = pool.stream().filter(e -> e != current).map(Entry::question).limit(4).toList();
        }
        return result;
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

package br.finedu.common;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtro de conteúdo para textos públicos (fórum, comentários, avaliações). Pensado para um
 * público de todas as idades, incluindo menores de idade:
 * <ul>
 *   <li>bloqueia dados de contato e documentos (telefone, e-mail, CPF), reduzindo riscos de
 *       aliciamento e exposição de dados pessoais;</li>
 *   <li>bloqueia termos ofensivos de uma lista básica;</li>
 *   <li>limita links (spam) e repetição excessiva de caracteres.</li>
 * </ul>
 * O texto é armazenado como texto puro e o frontend o exibe escapado (sem HTML).
 */
public final class ContentPolicy {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern PHONE = Pattern.compile("(\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\d{4}[-\\s.]?\\d{4}");
    private static final Pattern CPF = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");
    private static final Pattern LINK = Pattern.compile("(https?://|www\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{14,}");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final int MAX_LINKS = 2;

    /** Lista básica (sem acentos, minúsculas). Deve ser ampliada pela equipe de moderação. */
    private static final List<String> BLOCKED_WORDS = List.of(
            "idiota", "imbecil", "otario", "babaca", "retardado", "vagabundo", "vagabunda", "porra", "caralho",
            "puta", "merda", "viado", "macaco", "lixo humano", "se mata", "pix agora", "golpe garantido",
            "dinheiro facil", "renda garantida", "lucro garantido");

    private ContentPolicy() {
    }

    /** Remove caracteres de controle e espaços nas extremidades. */
    public static String sanitize(String text) {
        return text == null ? null : CONTROL.matcher(text).replaceAll("").strip();
    }

    /** Valida o texto e lança 422 com motivo compreensível quando violar a política. */
    public static String check(String text) {
        String clean = sanitize(text);
        if (clean == null || clean.isEmpty()) {
            return clean;
        }
        if (EMAIL.matcher(clean).find() || PHONE.matcher(clean).find() || CPF.matcher(clean).find()) {
            throw ApiException.business("PERSONAL_DATA",
                    "Por segurança, não publique e-mail, telefone ou documentos. Converse apenas pela plataforma.");
        }
        Matcher links = LINK.matcher(clean);
        int count = 0;
        while (links.find()) {
            count++;
        }
        if (count > MAX_LINKS) {
            throw ApiException.business("TOO_MANY_LINKS", "Use no máximo " + MAX_LINKS + " links por publicação.");
        }
        if (REPEATED.matcher(clean).find()) {
            throw ApiException.business("SPAM", "O texto parece conter repetições excessivas.");
        }
        String normalized = normalize(clean);
        for (String word : BLOCKED_WORDS) {
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(normalized).find()) {
                throw ApiException.business("INAPPROPRIATE_CONTENT",
                        "O texto contém termos não permitidos pelas regras da comunidade.");
            }
        }
        return clean;
    }

    static String normalize(String text) {
        String n = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}

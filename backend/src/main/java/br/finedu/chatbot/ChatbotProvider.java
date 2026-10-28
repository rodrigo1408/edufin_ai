package br.finedu.chatbot;

import java.util.List;

/**
 * Ponto de extensão do tutor. A implementação padrão é uma base de conhecimento local curada; um
 * provedor de IA generativa pode ser adicionado implementando esta interface (com chave em variável
 * de ambiente, timeout e tratamento de erros), sem alterar o controlador nem as salvaguardas do
 * {@link ChatbotService}, que sempre são aplicadas antes do provedor.
 */
public interface ChatbotProvider {

    /** Como o aluno quer a resposta. */
    enum Mode {
        /** Pergunta livre. */
        ASK,
        /** Outra forma de explicar o mesmo conceito da resposta anterior. */
        ALTERNATIVE,
        /** Exemplo prático do mesmo conceito da resposta anterior. */
        EXAMPLE
    }

    record Link(String label, String path) {
    }

    /**
     * @param question  texto livre (modo ASK)
     * @param entryId   resposta anterior a reformular/exemplificar (modos ALTERNATIVE e EXAMPLE)
     * @param topic     tema do contexto atual (aula ou exercício), usado para priorizar respostas; pode ser nulo
     */
    record Query(String question, Mode mode, String entryId, String topic) {
    }

    record ProviderAnswer(boolean matched, String entryId, String topic, String answer, List<Link> links,
                          List<String> suggestions, boolean hasAlternative, boolean hasExample) {
    }

    String name();

    ProviderAnswer answer(Query query);

    /** Explicação base de um tema (usada como dica de exercício, sem revelar a resposta). */
    ProviderAnswer conceptOf(String topic);

    /** Perguntas sugeridas, priorizando o tema informado (pode ser nulo). */
    List<String> suggestions(String topic, int limit);
}

package br.finedu.chatbot;

import java.util.List;

/**
 * Ponto de extensão do assistente. A implementação padrão é uma base de conhecimento local; um
 * provedor de IA externo pode ser adicionado implementando esta interface (com chave em variável
 * de ambiente, timeout e tratamento de erros), sem alterar o controlador nem as salvaguardas do
 * {@link ChatbotService}, que sempre são aplicadas antes do provedor.
 */
public interface ChatbotProvider {

    record Link(String label, String path) {
    }

    record ProviderAnswer(boolean matched, String topic, String answer, List<Link> links, List<String> suggestions) {
    }

    String name();

    ProviderAnswer answer(String question);
}

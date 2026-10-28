package br.finedu.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContentPolicyTest {

    @Test
    void acceptsRegularEducationalText() {
        assertThat(ContentPolicy.check("  Consegui guardar 20% da mesada com a regra 50-30-20!  "))
                .isEqualTo("Consegui guardar 20% da mesada com a regra 50-30-20!");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "me chama no zap 11 98765-4321",
            "meu email é fulano@gmail.com",
            "meu cpf 123.456.789-09"})
    void blocksPersonalContactData(String text) {
        assertThatThrownBy(() -> ContentPolicy.check(text)).isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("PERSONAL_DATA");
    }

    @Test
    void blocksOffensiveWordsIgnoringAccentsAndCase() {
        assertThatThrownBy(() -> ContentPolicy.check("Que IDIOTA")).extracting("code").isEqualTo("INAPPROPRIATE_CONTENT");
        assertThatThrownBy(() -> ContentPolicy.check("Lucro GARANTIDO aqui")).extracting("code")
                .isEqualTo("INAPPROPRIATE_CONTENT");
    }

    @Test
    void blocksSpamPatterns() {
        assertThatThrownBy(() -> ContentPolicy.check("http://a.com http://b.com http://c.com"))
                .extracting("code").isEqualTo("TOO_MANY_LINKS");
        assertThatThrownBy(() -> ContentPolicy.check("ganheeeeeeeeeeeeeeeeeeeee")).extracting("code").isEqualTo("SPAM");
    }

    @Test
    void sanitizeRemovesControlCharacters() {
        assertThat(ContentPolicy.sanitize("ok\u0000\u0007 texto")).isEqualTo("ok texto");
    }
}

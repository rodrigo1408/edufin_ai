package br.finedu.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class ErrorHandlingIntegrationTest extends IntegrationTest {

    @Test
    void malformedJsonGivesConsistentErrorWithoutInternals() throws Exception {
        TestUser u = register();
        postAs(u, "/api/goals", "{\"title\": ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.finedu"))));
    }

    @Test
    void typeMismatchAndUnknownEnum() throws Exception {
        TestUser u = register();
        getAs(u, "/api/goals/abc").andExpect(status().isBadRequest());
        getAs(u, "/api/transactions?type=ROUBO").andExpect(status().isBadRequest());
        postAs(u, "/api/goals", "{\"title\":\"x\",\"category\":\"INEXISTENTE\",\"targetAmount\":10}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownApiPathIsJson404() throws Exception {
        TestUser u = register();
        getAs(u, "/api/nao-existe").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void sqlInjectionAttemptsAreTreatedAsPlainText() throws Exception {
        TestUser u = register();
        getAs(u, "/api/transactions?q=' OR 1=1 --").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        getAs(u, "/api/forum/posts?q=%25' ; DROP TABLE users; --").andExpect(status().isOk());
        getAs(u, "/api/me").andExpect(status().isOk());
    }

    @Test
    void oversizedPayloadsAreRejected() throws Exception {
        TestUser u = register();
        String longTitle = "a".repeat(200);
        postAs(u, "/api/goals", "{\"title\":\"%s\",\"category\":\"OTHER\",\"targetAmount\":10}".formatted(longTitle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }
}

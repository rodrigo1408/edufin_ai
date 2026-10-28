package br.finedu.feedback;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.security.Role;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class FeedbackIntegrationTest extends IntegrationTest {

    @Test
    void userSendsFeedbackAndAdminAnswers() throws Exception {
        TestUser u = register();
        long id = readLong(postAs(u, "/api/feedback",
                "{\"type\":\"PROBLEM\",\"subject\":\"Erro no gráfico\",\"message\":\"O gráfico não carregou no celular.\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("NEW")), "$.id");
        getAs(u, "/api/admin/feedback").andExpect(status().isForbidden());
        putAs(u, "/api/admin/feedback/" + id, "{\"status\":\"RESOLVED\"}").andExpect(status().isForbidden());

        TestUser admin = userWithRole(Role.ADMIN);
        getAs(admin, "/api/admin/feedback?status=NEW&type=PROBLEM&size=100")
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")]").exists());
        putAs(admin, "/api/admin/feedback/" + id, "{\"status\":\"RESOLVED\",\"response\":\"Corrigido, obrigado!\"}")
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        getAs(u, "/api/feedback").andExpect(jsonPath("$[0].adminResponse").value("Corrigido, obrigado!"));
        getAs(u, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='FEEDBACK_ANSWERED')]").exists());
    }

    @Test
    void validatesFeedback() throws Exception {
        TestUser u = register();
        postAs(u, "/api/feedback", "{\"type\":\"OTHER\",\"subject\":\"x\",\"message\":\"mensagem\"}")
                .andExpect(status().isBadRequest());
        postAs(u, "/api/feedback", "{\"type\":\"SUGGESTION\",\"subject\":\"\",\"message\":\"m\"}")
                .andExpect(status().isBadRequest());
    }
}

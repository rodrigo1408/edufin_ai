package br.finedu.privacy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.security.Role;
import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PrivacyIntegrationTest extends IntegrationTest {

    @Test
    void exportContainsOwnDataWithoutSecrets() throws Exception {
        TestUser u = register();
        postAs(u, "/api/transactions", """
                {"description":"Mesada","amount":100,"type":"INCOME","category":"ALLOWANCE","occurredOn":"%s"}
                """.formatted(LocalDate.now()));
        getAs(u, "/api/me/export").andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.profile.email").value(u.email()))
                .andExpect(jsonPath("$.transactions[0].description").value("Mesada"))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("securityStamp"))));
    }

    @Test
    void deleteAccountRequiresPasswordAndRemovesAccess() throws Exception {
        TestUser u = register();
        postAs(u, "/api/feedback", "{\"type\":\"SUGGESTION\",\"subject\":\"Ideia\",\"message\":\"Mais cursos!\"}")
                .andExpect(status().isCreated());
        deleteAs(u, "/api/me", "{\"password\":\"errada123\"}").andExpect(status().isUnprocessableEntity());
        deleteAs(u, "/api/me", "{\"password\":\"%s\"}".formatted(PASSWORD)).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(u.email(), PASSWORD)))
                .andExpect(status().isUnauthorized());
        TestUser admin = userWithRole(Role.ADMIN);
        getAs(admin, "/api/admin/feedback?size=100")
                .andExpect(jsonPath("$.content[?(@.subject=='Ideia' && @.author=='Conta excluída')]").exists());
    }
}

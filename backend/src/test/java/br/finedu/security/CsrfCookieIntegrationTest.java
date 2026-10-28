package br.finedu.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Contexto isolado: o pós-processador csrf() do spring-security-test substitui o repositório de
 * tokens do filtro no contexto compartilhado; aqui verificamos o comportamento real (cookie
 * XSRF-TOKEN legível pelo SPA, sem HttpOnly).
 */
@TestPropertySource(properties = "app.test.context=csrf-cookie")
class CsrfCookieIntegrationTest extends IntegrationTest {

    @Test
    void csrfEndpointIssuesReadableCookieForTheSpa() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}

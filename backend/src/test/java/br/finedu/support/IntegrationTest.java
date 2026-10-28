package br.finedu.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import br.finedu.security.Role;
import br.finedu.user.UserService;
import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base dos testes de integração: sobe a aplicação completa (segurança, MVC, JPA, Flyway em H2)
 * e autentica usuários reais pelo endpoint de login, usando a sessão HTTP resultante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    public static final String PASSWORD = "Senha123forte";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserService userService;

    public record TestUser(Long id, String email, MockHttpSession session) {
    }

    protected static String unique(String prefix) {
        return prefix + System.nanoTime() % 1_000_000 + SEQ.incrementAndGet();
    }

    /** Cadastra pelo endpoint público e devolve a sessão autenticada. */
    protected TestUser register() throws Exception {
        String name = unique("user");
        String email = name + "@teste.com";
        MockHttpSession session = new MockHttpSession();
        MvcResult r = mvc.perform(post("/api/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"%s","acceptTerms":true,"ageConfirmation":true}
                                """.formatted(email, PASSWORD, name)))
                .andReturn();
        if (r.getResponse().getStatus() != 201) {
            throw new IllegalStateException("Cadastro falhou: " + r.getResponse().getContentAsString());
        }
        Long id = ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
        return new TestUser(id, email, (MockHttpSession) r.getRequest().getSession(false));
    }

    /** Cria um usuário com papel específico (via serviço) e faz login pelo endpoint. */
    protected TestUser userWithRole(Role role) throws Exception {
        String name = unique(role.name().toLowerCase());
        String email = name + "@teste.com";
        Long id = userService.createUser(email, PASSWORD, name, role).getId();
        return new TestUser(id, email, login(email, PASSWORD));
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult r = mvc.perform(post("/api/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
        if (r.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Login falhou: " + r.getResponse().getContentAsString());
        }
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    protected ResultActions getAs(TestUser u, String url) throws Exception {
        return mvc.perform(get(url).session(u.session()));
    }

    protected ResultActions postAs(TestUser u, String url, String json) throws Exception {
        return mvc.perform(withJson(post(url), json).session(u.session()).with(csrf()));
    }

    protected ResultActions putAs(TestUser u, String url, String json) throws Exception {
        return mvc.perform(withJson(put(url), json).session(u.session()).with(csrf()));
    }

    protected ResultActions deleteAs(TestUser u, String url) throws Exception {
        return mvc.perform(delete(url).session(u.session()).with(csrf()));
    }

    protected ResultActions deleteAs(TestUser u, String url, String json) throws Exception {
        return mvc.perform(withJson(delete(url), json).session(u.session()).with(csrf()));
    }

    protected static MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder b, String json) {
        return json == null ? b : b.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    protected static long readLong(ResultActions result, String path) throws Exception {
        return ((Number) read(result, path)).longValue();
    }
}

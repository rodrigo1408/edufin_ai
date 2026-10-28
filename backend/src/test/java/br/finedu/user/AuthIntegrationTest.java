package br.finedu.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.security.Role;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerLogsInAndNeverExposesSecrets() throws Exception {
        TestUser u = register();
        getAs(u, "/api/me").andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(u.email()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.preferences.weeklyLessonTarget").value(3))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("securityStamp"))));
    }

    @Test
    void registrationIgnoresAttemptToChooseRole() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"hacker%d@teste.com","password":"Senha123forte","displayName":"hacker%d",
                                 "acceptTerms":true,"ageConfirmation":true,"role":"ADMIN"}
                                """.formatted(System.nanoTime() % 100000, System.nanoTime() % 100000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalido","password":"123","displayName":"x","acceptTerms":false,"ageConfirmation":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        TestUser u = register();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Senha123forte","displayName":"%s","acceptTerms":true,"ageConfirmation":true}
                                """.formatted(u.email().toUpperCase(), unique("outro"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_IN_USE"));
    }

    @Test
    void wrongPasswordGivesGenericError() throws Exception {
        TestUser u = register();
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"errada123\"}".formatted(u.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"naoexiste@teste.com\",\"password\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    void privateEndpointsRequireAuthentication() throws Exception {
        for (String url : new String[]{"/api/me", "/api/goals", "/api/transactions", "/api/dashboard", "/api/reports",
                "/api/notifications", "/api/wallets", "/api/forum/posts", "/api/quizzes", "/api/courses"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        TestUser u = register();
        mvc.perform(post("/api/goals").session(u.session()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"category\":\"OTHER\",\"targetAmount\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        TestUser u = register();
        mvc.perform(post("/api/auth/logout").session(u.session()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(u.session())).andExpect(status().isUnauthorized());
    }

    @Test
    void changingPasswordEndsOtherSessions() throws Exception {
        TestUser u = register();
        MockHttpSession other = login(u.email(), PASSWORD);
        putAs(u, "/api/me/password", "{\"currentPassword\":\"%s\",\"newPassword\":\"NovaSenha456\"}".formatted(PASSWORD))
                .andExpect(status().isNoContent());
        getAs(u, "/api/me").andExpect(status().isOk());
        mvc.perform(get("/api/me").session(other)).andExpect(status().isUnauthorized());
        login(u.email(), "NovaSenha456");
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        TestUser u = register();
        putAs(u, "/api/me/password", "{\"currentPassword\":\"errada999\",\"newPassword\":\"NovaSenha456\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
    }

    @Test
    void passwordResetIsHonestWhenEmailIsNotConfigured() throws Exception {
        mvc.perform(get("/api/public/config")).andExpect(jsonPath("$.passwordResetByEmail").value(false));
        mvc.perform(post("/api/auth/password-reset/request").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alguem@teste.com\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("RESET_UNAVAILABLE"));
    }

    @Test
    void adminGeneratedResetLinkAllowsSingleUsePasswordChange() throws Exception {
        TestUser admin = userWithRole(Role.ADMIN);
        TestUser u = register();
        String url = read(postAs(admin, "/api/admin/users/" + u.id() + "/reset-link", null).andExpect(status().isOk()),
                "$.resetUrl");
        String token = url.substring(url.indexOf("token=") + 6);
        String body = "{\"token\":\"%s\",\"newPassword\":\"Recuperada789\"}".formatted(token);
        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/password-reset/confirm").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mvc.perform(get("/api/me").session(u.session())).andExpect(status().isUnauthorized());
        login(u.email(), "Recuperada789");
    }

    @Test
    void preferencesAndProfileUpdate() throws Exception {
        TestUser u = register();
        putAs(u, "/api/me/preferences", """
                {"theme":"DARK","notifyGoals":false,"notifyLearning":true,"notifyAchievements":true,
                 "notifyCommunity":false,"weeklyLessonTarget":5}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("DARK"));
        putAs(u, "/api/me", "{\"displayName\":\"%s\"}".formatted(unique("novo"))).andExpect(status().isOk());
        getAs(u, "/api/me/activity").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("PROFILE_UPDATED"));
        putAs(u, "/api/me/preferences", "{\"theme\":\"DARK\",\"weeklyLessonTarget\":0}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabledAccountLosesSessionAndCannotLogin() throws Exception {
        TestUser admin = userWithRole(Role.ADMIN);
        TestUser u = register();
        putAs(admin, "/api/admin/users/" + u.id() + "/status", "{\"enabled\":false}").andExpect(status().isOk());
        mvc.perform(get("/api/me").session(u.session())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(u.email(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}

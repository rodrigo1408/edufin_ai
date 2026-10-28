package br.finedu.admin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.security.Role;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class AdminSecurityIntegrationTest extends IntegrationTest {

    @Test
    void regularUsersCannotReachAdminArea() throws Exception {
        TestUser u = register();
        for (String url : new String[]{"/api/admin/stats", "/api/admin/users", "/api/admin/content/courses",
                "/api/admin/content/quizzes", "/api/admin/moderation/reports", "/api/admin/feedback"}) {
            getAs(u, url).andExpect(status().isForbidden());
        }
        putAs(u, "/api/admin/users/" + u.id() + "/role", "{\"role\":\"ADMIN\"}").andExpect(status().isForbidden());
        getAs(u, "/api/me").andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void permissionsAreGranularPerRole() throws Exception {
        TestUser educator = userWithRole(Role.EDUCATOR);
        getAs(educator, "/api/admin/content/quizzes").andExpect(status().isOk());
        getAs(educator, "/api/admin/users").andExpect(status().isForbidden());
        getAs(educator, "/api/admin/moderation/reports").andExpect(status().isForbidden());
        getAs(educator, "/api/admin/stats").andExpect(status().isOk()).andExpect(jsonPath("$.users").doesNotExist())
                .andExpect(jsonPath("$.courses").value(6));

        TestUser moderator = userWithRole(Role.MODERATOR);
        getAs(moderator, "/api/admin/moderation/reports").andExpect(status().isOk());
        getAs(moderator, "/api/admin/content/courses").andExpect(status().isForbidden());
        getAs(moderator, "/api/admin/feedback").andExpect(status().isForbidden());
    }

    @Test
    void adminManagesRolesWithSafeguards() throws Exception {
        TestUser admin = userWithRole(Role.ADMIN);
        TestUser target = register();
        putAs(admin, "/api/admin/users/" + admin.id() + "/role", "{\"role\":\"USER\"}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SELF_CHANGE"));
        putAs(admin, "/api/admin/users/" + target.id() + "/role", "{\"role\":\"EDUCATOR\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EDUCATOR"));
        // Sessão antiga do usuário é encerrada ao mudar o papel; novo login traz as novas permissões.
        getAs(target, "/api/me").andExpect(status().isUnauthorized());
        TestUser relogged = new TestUser(target.id(), target.email(), login(target.email(), PASSWORD));
        getAs(relogged, "/api/admin/content/courses").andExpect(status().isOk());
        getAs(admin, "/api/admin/users?q=" + target.email().substring(0, 8)).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void educatorManagesQuizContent() throws Exception {
        TestUser educator = userWithRole(Role.EDUCATOR);
        String quiz = """
                {"quiz":{"title":"Quiz de teste","description":"Descrição","category":"SAVING","difficulty":"BEGINNER",
                 "published":true},
                 "questions":[{"statement":"Pergunta 1?","explanation":"Porque sim.","topic":"SAVING",
                   "options":[{"text":"A","correct":true},{"text":"B","correct":false}]}]}
                """;
        long id = readLong(postAs(educator, "/api/admin/content/quizzes", quiz).andExpect(status().isCreated()), "$.id");
        postAs(educator, "/api/admin/content/quizzes", quiz.replace("\"correct\":false", "\"correct\":true"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
        putAs(educator, "/api/admin/content/quizzes/" + id + "/questions", """
                {"questions":[{"statement":"Nova?","explanation":"Ok.","topic":"SAVING",
                  "options":[{"text":"X","correct":false},{"text":"Y","correct":true},{"text":"Z","correct":false}]}]}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].options.length()").value(3));

        TestUser student = register();
        long attempt = readLong(postAs(student, "/api/quizzes/" + id + "/attempts", null), "$.attemptId");
        putAs(educator, "/api/admin/content/quizzes/" + id + "/questions", """
                {"questions":[{"statement":"Outra?","explanation":"Ok.","topic":"SAVING",
                  "options":[{"text":"X","correct":true},{"text":"Y","correct":false}]}]}
                """).andExpect(status().isConflict());
        deleteAs(educator, "/api/admin/content/quizzes/" + id).andExpect(status().isConflict());
        putAs(educator, "/api/admin/content/quizzes/" + id, """
                {"title":"Quiz de teste","description":"Descrição","category":"SAVING","difficulty":"BEGINNER","published":false}
                """).andExpect(jsonPath("$.published").value(false));
        postAs(student, "/api/quizzes/" + id + "/attempts", null).andExpect(status().isNotFound());
        getAs(student, "/api/quiz-attempts/" + attempt).andExpect(status().isOk());
    }

    @Test
    void educatorBuildsACourse() throws Exception {
        TestUser educator = userWithRole(Role.EDUCATOR);
        String slug = "curso-" + System.nanoTime() % 100000;
        long id = readLong(postAs(educator, "/api/admin/content/courses", """
                {"slug":"%s","title":"Curso novo","summary":"Resumo","description":"Descrição","objectives":"Objetivo 1",
                 "category":"BUDGET","difficulty":"BEGINNER","estimatedMinutes":10,"position":99,"published":false}
                """.formatted(slug)).andExpect(status().isCreated()), "$.id");
        long moduleId = readLong(postAs(educator, "/api/admin/content/courses/" + id + "/modules", "{\"title\":\"Módulo 1\"}"),
                "$.modules[0].id");
        postAs(educator, "/api/admin/content/modules/" + moduleId + "/lessons",
                "{\"title\":\"Aula 1\",\"content\":\"## Conteúdo\",\"estimatedMinutes\":5}")
                .andExpect(jsonPath("$.modules[0].lessons[0].title").value("Aula 1"));
        TestUser student = register();
        getAs(student, "/api/courses/" + id).andExpect(status().isNotFound()); // não publicado
        deleteAs(educator, "/api/admin/content/modules/" + moduleId).andExpect(status().isUnprocessableEntity());
        deleteAs(educator, "/api/admin/content/courses/" + id).andExpect(status().isNoContent());
    }
}

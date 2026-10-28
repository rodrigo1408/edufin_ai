package br.finedu.forum;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.security.Role;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class ForumIntegrationTest extends IntegrationTest {

    private long post(TestUser u, String title) throws Exception {
        return readLong(postAs(u, "/api/forum/posts", """
                {"category":"TIPS","title":"%s","body":"Separei 20%% da mesada assim que recebi e deu certo."}
                """.formatted(title)).andExpect(status().isCreated()), "$.post.id");
    }

    @Test
    void createSearchCommentAndLike() throws Exception {
        TestUser author = register();
        TestUser reader = register();
        String title = unique("Dica de economia ");
        long id = post(author, title);
        getAs(reader, "/api/forum/posts?q=" + title.substring(0, 18)).andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].mine").value(false));
        getAs(reader, "/api/forum/posts?category=TIPS&size=50").andExpect(jsonPath("$.content[?(@.id==" + id + ")]").exists());

        postAs(reader, "/api/forum/posts/" + id + "/comments", "{\"body\":\"Boa ideia!\"}").andExpect(status().isCreated());
        getAs(author, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='POST_COMMENTED')]").exists());

        postAs(reader, "/api/forum/posts/" + id + "/like", null).andExpect(jsonPath("$.likes").value(1));
        postAs(reader, "/api/forum/posts/" + id + "/like", null).andExpect(jsonPath("$.likes").value(1));
        getAs(reader, "/api/forum/posts/" + id).andExpect(jsonPath("$.post.likedByMe").value(true))
                .andExpect(jsonPath("$.post.comments").value(1))
                .andExpect(jsonPath("$.post.author").isNotEmpty());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/forum/posts/" + id + "/like").session(reader.session())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(jsonPath("$.likes").value(0));
    }

    @Test
    void contentPolicyBlocksPersonalData() throws Exception {
        TestUser u = register();
        postAs(u, "/api/forum/posts", """
                {"category":"QUESTIONS","title":"Me chama","body":"meu whats 11 98765-4321 pra conversar"}
                """).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PERSONAL_DATA"));
        postAs(u, "/api/forum/posts", "{\"category\":\"QUESTIONS\",\"title\":\"oi\",\"body\":\"curto\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAuthorCanEditOrDelete() throws Exception {
        TestUser author = register();
        TestUser other = register();
        long id = post(author, "Post do autor");
        putAs(other, "/api/forum/posts/" + id, "{\"category\":\"TIPS\",\"title\":\"Hackeado\",\"body\":\"Texto alterado indevidamente\"}")
                .andExpect(status().isForbidden());
        deleteAs(other, "/api/forum/posts/" + id).andExpect(status().isForbidden());
        putAs(author, "/api/forum/posts/" + id, "{\"category\":\"TIPS\",\"title\":\"Post editado\",\"body\":\"Texto revisado pelo autor.\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.post.edited").value(true));
        deleteAs(author, "/api/forum/posts/" + id).andExpect(status().isNoContent());
        getAs(author, "/api/forum/posts/" + id).andExpect(status().isNotFound());
    }

    @Test
    void reportsAutoHideAndModeratorsDecide() throws Exception {
        TestUser author = register();
        long id = post(author, "Post polêmico");
        postAs(author, "/api/forum/posts/" + id + "/report", "{\"reason\":\"SPAM\"}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("OWN_CONTENT"));
        TestUser r1 = register();
        postAs(r1, "/api/forum/posts/" + id + "/report", "{\"reason\":\"SPAM\"}").andExpect(status().isAccepted());
        postAs(r1, "/api/forum/posts/" + id + "/report", "{\"reason\":\"SPAM\"}").andExpect(status().isConflict());
        postAs(register(), "/api/forum/posts/" + id + "/report", "{\"reason\":\"OFFENSIVE\"}");
        getAs(r1, "/api/forum/posts/" + id).andExpect(status().isOk());
        postAs(register(), "/api/forum/posts/" + id + "/report", "{\"reason\":\"SCAM\"}");
        getAs(r1, "/api/forum/posts/" + id).andExpect(status().isNotFound()); // ocultado automaticamente

        getAs(r1, "/api/admin/moderation/reports").andExpect(status().isForbidden());
        TestUser moderator = userWithRole(Role.MODERATOR);
        java.util.List<Number> reportIds = read(getAs(moderator, "/api/admin/moderation/reports?size=100"),
                "$.content[?(@.targetId==" + id + ")].id");
        long reportId = reportIds.get(0).longValue();
        postAs(moderator, "/api/admin/moderation/reports/" + reportId + "/resolve", "{\"action\":\"RESTORE\"}")
                .andExpect(status().isOk());
        getAs(r1, "/api/forum/posts/" + id).andExpect(status().isOk());
        postAs(moderator, "/api/admin/moderation/posts/" + id, "{\"action\":\"REMOVE\"}").andExpect(status().isNoContent());
        getAs(r1, "/api/forum/posts/" + id).andExpect(status().isNotFound());
        getAs(author, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='CONTENT_MODERATED')]").exists());
    }
}

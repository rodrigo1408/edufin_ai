package br.finedu.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.quiz.QuizOption;
import br.finedu.quiz.QuizQuestion;
import br.finedu.quiz.QuizRepository;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class TutorIntegrationTest extends IntegrationTest {

    @Autowired
    QuizRepository quizzes;

    @Autowired
    TransactionTemplate tx;

    @Test
    void lessonContextPrioritizesTheLessonTopic() throws Exception {
        TestUser u = register();
        long courseId = readLong(getAs(u, "/api/courses?topic=INTEREST"), "$[0].id");
        postAs(u, "/api/courses/" + courseId + "/enroll", null);
        long lessonId = readLong(getAs(u, "/api/courses/" + courseId), "$.modules[0].lessons[0].id");

        getAs(u, "/api/chatbot/info?lessonId=" + lessonId).andExpect(status().isOk())
                .andExpect(jsonPath("$.context.topic").value("INTEREST"))
                .andExpect(jsonPath("$.generativeModel").value(false))
                .andExpect(jsonPath("$.suggestions[0]").value("O que são juros?"));

        String entry = read(postAs(u, "/api/chatbot/ask", "{\"message\":\"me explica isso com calma\",\"lessonId\":" + lessonId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.title").isNotEmpty()), "$.context.kind");
        assertThat(entry).isEqualTo("LESSON");

        String id = read(postAs(u, "/api/chatbot/ask", "{\"message\":\"o que são juros compostos?\",\"lessonId\":" + lessonId + "}")
                .andExpect(jsonPath("$.topic").value("INTEREST"))
                .andExpect(jsonPath("$.canGiveExample").value(true)), "$.entryId");
        postAs(u, "/api/chatbot/ask", "{\"mode\":\"EXAMPLE\",\"entryId\":\"" + id + "\",\"lessonId\":" + lessonId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("EXAMPLE"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.startsWith("Exemplo:")));
    }

    @Test
    void quizHintExplainsTheConceptWithoutRevealingTheAnswer() throws Exception {
        TestUser u = register();
        long quizId = readLong(getAs(u, "/api/quizzes?topic=INTEREST"), "$[0].id");
        record Q(long id, String correctText, String explanation) {
        }
        Q q = tx.execute(s -> {
            QuizQuestion question = quizzes.findById(quizId).orElseThrow().getQuestions().get(0);
            QuizOption correct = question.correctOption().orElseThrow();
            return new Q(question.getId(), correct.getText(), question.getExplanation());
        });
        String body = postAs(u, "/api/chatbot/hint/question/" + q.id(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.context.kind").value("QUESTION"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("sem entregar a resposta")))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(q.correctText()).doesNotContain(q.explanation());
        assertThat(body).doesNotContain("correctOptionId").doesNotContain("\"correct\"");
    }

    @Test
    void tutorRequiresAuthenticationAndValidContext() throws Exception {
        mvc.perform(get("/api/chatbot/info")).andExpect(status().isUnauthorized());
        TestUser u = register();
        postAs(u, "/api/chatbot/ask", "{\"mode\":\"ALTERNATIVE\"}").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MISSING_CONTEXT"));
        postAs(u, "/api/chatbot/ask", "{\"message\":\"oi\",\"lessonId\":999999}").andExpect(status().isNotFound());
        postAs(u, "/api/chatbot/hint/question/999999", null).andExpect(status().isNotFound());
    }

    @Test
    void publicCourseListShowsOnlyCatalogSummaries() throws Exception {
        mvc.perform(get("/api/public/courses")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].lessonCount").value(4))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }
}

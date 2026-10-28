package br.finedu.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class QuizIntegrationTest extends IntegrationTest {

    @Autowired
    QuizRepository quizzes;

    @Autowired
    TransactionTemplate tx;

    record Answer(long questionId, long correctOption, long wrongOption) {
    }

    /** Gabarito lido diretamente do banco (o teste simula alguém que conhece as respostas). */
    private List<Answer> answerKey(long quizId) {
        return tx.execute(s -> quizzes.findById(quizId).orElseThrow().getQuestions().stream()
                .map(q -> new Answer(q.getId(), q.correctOption().orElseThrow().getId(),
                        q.getOptions().stream().filter(o -> !o.isCorrect()).findFirst().orElseThrow().getId()))
                .toList());
    }

    private long firstQuizId(TestUser u) throws Exception {
        return readLong(getAs(u, "/api/quizzes?topic=BUDGET"), "$[0].id");
    }

    @Test
    void attemptNeverRevealsCorrectAnswersBeforeAnswering() throws Exception {
        TestUser u = register();
        long quizId = firstQuizId(u);
        postAs(u, "/api/quizzes/" + quizId + "/attempts", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[0].options[0].text").exists())
                .andExpect(content().string(not(containsString("correct"))))
                .andExpect(content().string(not(containsString("explanation"))));
    }

    @Test
    void scoreIsComputedByServerFromStoredAnswers() throws Exception {
        TestUser u = register();
        long quizId = firstQuizId(u);
        long attemptId = readLong(postAs(u, "/api/quizzes/" + quizId + "/attempts", null), "$.attemptId");
        List<Answer> key = answerKey(quizId);
        for (int i = 0; i < key.size(); i++) {
            Answer a = key.get(i);
            long chosen = i < 3 ? a.correctOption() : a.wrongOption();
            postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                    "{\"questionId\":%d,\"optionId\":%d}".formatted(a.questionId(), chosen))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correct").value(i < 3))
                    .andExpect(jsonPath("$.correctOptionId").value(a.correctOption()))
                    .andExpect(jsonPath("$.explanation").isNotEmpty());
        }
        // O corpo enviado pelo cliente é ignorado: não há como informar a nota.
        postAs(u, "/api/quiz-attempts/" + attemptId + "/finish", "{\"score\":999,\"percent\":100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(3))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.score").value(30))
                .andExpect(jsonPath("$.percent").value(60.0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.weakTopics").isNotEmpty())
                .andExpect(jsonPath("$.review.length()").value(5));

        postAs(u, "/api/quiz-attempts/" + attemptId + "/finish", null).andExpect(status().isUnprocessableEntity());
        postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                "{\"questionId\":%d,\"optionId\":%d}".formatted(key.get(0).questionId(), key.get(0).correctOption()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ATTEMPT_FINISHED"));

        getAs(u, "/api/quiz-attempts/history").andExpect(jsonPath("$.content[0].percent").value(60.0));
        getAs(u, "/api/quizzes/" + quizId).andExpect(jsonPath("$.quiz.bestPercent").value(60.0))
                .andExpect(jsonPath("$.myAttempts.length()").value(1));
    }

    @Test
    void answersCannotBeChangedOrForged() throws Exception {
        TestUser u = register();
        long quizId = firstQuizId(u);
        long attemptId = readLong(postAs(u, "/api/quizzes/" + quizId + "/attempts", null), "$.attemptId");
        List<Answer> key = answerKey(quizId);
        Answer a = key.get(0);
        postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                "{\"questionId\":%d,\"optionId\":%d}".formatted(a.questionId(), a.wrongOption())).andExpect(status().isOk());
        postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                "{\"questionId\":%d,\"optionId\":%d}".formatted(a.questionId(), a.correctOption()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_ANSWERED"));
        // Alternativa de outra pergunta
        postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                "{\"questionId\":%d,\"optionId\":%d}".formatted(key.get(1).questionId(), a.correctOption()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_OPTION"));
        // Retomar a tentativa devolve as respostas já dadas
        postAs(u, "/api/quizzes/" + quizId + "/attempts", null)
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.answered.length()").value(1));
    }

    @Test
    void otherUsersCannotTouchAnAttempt() throws Exception {
        TestUser owner = register();
        TestUser intruder = register();
        long quizId = firstQuizId(owner);
        long attemptId = readLong(postAs(owner, "/api/quizzes/" + quizId + "/attempts", null), "$.attemptId");
        Answer a = answerKey(quizId).get(0);
        postAs(intruder, "/api/quiz-attempts/" + attemptId + "/answers",
                "{\"questionId\":%d,\"optionId\":%d}".formatted(a.questionId(), a.correctOption()))
                .andExpect(status().isNotFound());
        postAs(intruder, "/api/quiz-attempts/" + attemptId + "/finish", null).andExpect(status().isNotFound());
        getAs(intruder, "/api/quiz-attempts/" + attemptId).andExpect(status().isNotFound());
    }

    @Test
    void perfectScoreAwardsPointsOnlyOncePerQuiz() throws Exception {
        TestUser u = register();
        long quizId = firstQuizId(u);
        List<Answer> key = answerKey(quizId);
        for (int round = 0; round < 3; round++) {
            long attemptId = readLong(postAs(u, "/api/quizzes/" + quizId + "/attempts", null), "$.attemptId");
            for (Answer a : key) {
                postAs(u, "/api/quiz-attempts/" + attemptId + "/answers",
                        "{\"questionId\":%d,\"optionId\":%d}".formatted(a.questionId(), a.correctOption()));
            }
            var result = postAs(u, "/api/quiz-attempts/" + attemptId + "/finish", null)
                    .andExpect(jsonPath("$.percent").value(100.0)).andExpect(jsonPath("$.passed").value(true));
            if (round == 0) {
                assertThat(((Number) read(result, "$.reward.points")).intValue()).isGreaterThanOrEqualTo(25);
            }
        }
        var history = getAs(u, "/api/gamification/history?size=100");
        List<?> perfect = read(history, "$.content[?(@.type=='QUIZ_PERFECT')]");
        List<?> pass = read(history, "$.content[?(@.type=='QUIZ_FIRST_PASS')]");
        List<?> challenges = read(history, "$.content[?(@.type=='CHALLENGE_COMPLETED')]");
        assertThat(perfect).hasSize(1);
        assertThat(pass).hasSize(1);
        assertThat(challenges).hasSize(1); // "2 quizzes na semana": concedido uma única vez
        getAs(u, "/api/gamification/summary")
                .andExpect(jsonPath("$.achievements[?(@.code=='QUIZ_PERFECT')].unlocked").value(true));
    }
}

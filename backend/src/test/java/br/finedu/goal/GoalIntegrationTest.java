package br.finedu.goal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GoalIntegrationTest extends IntegrationTest {

    private long createGoal(TestUser u, String target) throws Exception {
        return readLong(postAs(u, "/api/goals", """
                {"title":"Fone novo","description":"Fone bluetooth","category":"ELECTRONICS","targetAmount":%s,
                 "deadline":"%s"}
                """.formatted(target, LocalDate.now().plusMonths(6))).andExpect(status().isCreated()), "$.goal.id");
    }

    @Test
    void fullGoalLifecycleWithAutomaticCompletion() throws Exception {
        TestUser u = register();
        long id = createGoal(u, "300.00");
        getAs(u, "/api/goals/" + id).andExpect(jsonPath("$.savedAmount").value(0.0))
                .andExpect(jsonPath("$.progressPercent").value(0.0))
                .andExpect(jsonPath("$.suggestedMonthlySaving").isNumber());

        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":100.50}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goal.savedAmount").value(100.50))
                .andExpect(jsonPath("$.goal.progressPercent").value(33.5))
                .andExpect(jsonPath("$.goalCompleted").value(false));

        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"WITHDRAWAL\",\"amount\":200}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":250}")
                .andExpect(jsonPath("$.goal.status").value("COMPLETED"))
                .andExpect(jsonPath("$.goal.progressPercent").value(100.0))
                .andExpect(jsonPath("$.goal.remainingAmount").value(0.0))
                .andExpect(jsonPath("$.goalCompleted").value(true))
                .andExpect(jsonPath("$.reward.points").isNumber());

        getAs(u, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='GOAL_COMPLETED')]").exists());

        // Retirada que mantém o saldo igual ao alvo: continua concluída
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"WITHDRAWAL\",\"amount\":50.50}")
                .andExpect(jsonPath("$.goal.status").value("COMPLETED"))
                .andExpect(jsonPath("$.goal.savedAmount").value(300.0));
        // Retirada que deixa o saldo abaixo do alvo: a meta volta a ficar ativa
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"WITHDRAWAL\",\"amount\":0.01}")
                .andExpect(jsonPath("$.goal.status").value("ACTIVE"))
                .andExpect(jsonPath("$.goal.savedAmount").value(299.99));

        getAs(u, "/api/goals/" + id + "/movements").andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"));
    }

    @Test
    void loweringTargetBelowBalanceCompletesGoal() throws Exception {
        TestUser u = register();
        long id = createGoal(u, "500");
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":120}");
        putAs(u, "/api/goals/" + id, """
                {"title":"Fone simples","category":"ELECTRONICS","targetAmount":100}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void cancelledGoalRejectsMovementsAndEdits() throws Exception {
        TestUser u = register();
        long id = createGoal(u, "100");
        postAs(u, "/api/goals/" + id + "/cancel", null).andExpect(jsonPath("$.status").value("CANCELLED"));
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":10}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GOAL_CANCELLED"));
        getAs(u, "/api/goals?status=CANCELLED").andExpect(jsonPath("$.length()").value(1));
        getAs(u, "/api/goals?status=ACTIVE").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidValuesAreRejected() throws Exception {
        TestUser u = register();
        postAs(u, "/api/goals", "{\"title\":\"x\",\"category\":\"OTHER\",\"targetAmount\":0}")
                .andExpect(status().isBadRequest());
        postAs(u, "/api/goals", "{\"title\":\"x\",\"category\":\"OTHER\",\"targetAmount\":-5}")
                .andExpect(status().isBadRequest());
        postAs(u, "/api/goals", "{\"title\":\"x\",\"category\":\"OTHER\",\"targetAmount\":10.123}")
                .andExpect(status().isBadRequest());
        postAs(u, "/api/goals", "{\"title\":\"x\",\"category\":\"OTHER\",\"targetAmount\":10,\"deadline\":\"2000-01-01\"}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_DEADLINE"));
        long id = createGoal(u, "100");
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":-10}")
                .andExpect(status().isBadRequest());
        postAs(u, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":0}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeOrChangeOtherUsersGoals() throws Exception {
        TestUser owner = register();
        TestUser intruder = register();
        long id = createGoal(owner, "100");
        getAs(intruder, "/api/goals/" + id).andExpect(status().isNotFound());
        getAs(intruder, "/api/goals/" + id + "/movements").andExpect(status().isNotFound());
        putAs(intruder, "/api/goals/" + id, "{\"title\":\"hack\",\"category\":\"OTHER\",\"targetAmount\":1}")
                .andExpect(status().isNotFound());
        postAs(intruder, "/api/goals/" + id + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":10}")
                .andExpect(status().isNotFound());
        postAs(intruder, "/api/goals/" + id + "/cancel", null).andExpect(status().isNotFound());
        getAs(intruder, "/api/goals").andExpect(jsonPath("$.length()").value(0));
        getAs(owner, "/api/goals/" + id).andExpect(jsonPath("$.savedAmount").value(0.0));
    }
}

package br.finedu.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GamificationIntegrationTest extends IntegrationTest {

    @Test
    void dailyHabitPointsAreGrantedOncePerDay() throws Exception {
        TestUser u = register();
        int first = 0;
        for (int i = 0; i < 5; i++) {
            var r = postAs(u, "/api/transactions", """
                    {"description":"Lanche %d","amount":5,"type":"EXPENSE","category":"FOOD","occurredOn":"%s"}
                    """.formatted(i, LocalDate.now())).andExpect(status().isCreated());
            int points = ((Number) read(r, "$.reward.points")).intValue();
            if (i == 0) {
                first = points;
            } else {
                assertThat(points).isZero();
            }
        }
        assertThat(first).isEqualTo(2);
        List<?> logged = read(getAs(u, "/api/gamification/history?size=50"), "$.content[?(@.type=='TRANSACTION_LOGGED')]");
        assertThat(logged).hasSize(1);
    }

    @Test
    void creatingAndCancellingGoalsCannotFarmPoints() throws Exception {
        TestUser u = register();
        long total = 0;
        for (int i = 0; i < 4; i++) {
            var r = postAs(u, "/api/goals", "{\"title\":\"Meta %d\",\"category\":\"OTHER\",\"targetAmount\":10}".formatted(i));
            total += ((Number) read(r, "$.reward.points")).longValue();
            postAs(u, "/api/goals/" + readLong(r, "$.goal.id") + "/cancel", null);
        }
        // 5 pontos do hábito diário + 10 da conquista "Sonhar com plano"
        assertThat(total).isEqualTo(15);
        getAs(u, "/api/gamification/summary").andExpect(jsonPath("$.totalPoints").value(15))
                .andExpect(jsonPath("$.achievementsUnlocked").value(1))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void summaryHasChallengesAndLevels() throws Exception {
        TestUser u = register();
        getAs(u, "/api/gamification/summary").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.levelTitle").value("Iniciante"))
                .andExpect(jsonPath("$.challenges.length()").value(3))
                .andExpect(jsonPath("$.achievementsTotal").value(14))
                .andExpect(jsonPath("$.currentStreak").value(0));
    }

    @Test
    void gamificationIsReadOnlyForClients() throws Exception {
        TestUser u = register();
        postAs(u, "/api/gamification/summary", "{\"totalPoints\":99999}").andExpect(status().isMethodNotAllowed());
    }
}

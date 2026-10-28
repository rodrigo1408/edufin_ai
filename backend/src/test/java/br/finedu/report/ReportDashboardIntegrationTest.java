package br.finedu.report;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ReportDashboardIntegrationTest extends IntegrationTest {

    @Test
    void newUserGetsEmptyDashboardWithGuidance() throws Exception {
        TestUser u = register();
        getAs(u, "/api/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.goals.active").value(0))
                .andExpect(jsonPath("$.goals.totalSaved").value(0.0))
                .andExpect(jsonPath("$.monthFinances.income").value(0.0))
                .andExpect(jsonPath("$.hasTransactions").value(false))
                .andExpect(jsonPath("$.learning.enrolled").value(0))
                .andExpect(jsonPath("$.quizzes.attempts").value(0))
                .andExpect(jsonPath("$.unreadNotifications").value(1)) // boas-vindas
                .andExpect(jsonPath("$.recommendations[?(@.kind=='GOAL')]").exists())
                .andExpect(jsonPath("$.recommendations[?(@.kind=='HABIT')]").exists());
    }

    @Test
    void dashboardAndReportsReflectPersistedData() throws Exception {
        TestUser u = register();
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1).withDayOfMonth(10);
        postAs(u, "/api/transactions", """
                {"description":"Mesada","amount":300,"type":"INCOME","category":"ALLOWANCE","occurredOn":"%s"}
                """.formatted(lastMonth));
        postAs(u, "/api/transactions", """
                {"description":"Jogo","amount":80,"type":"EXPENSE","category":"TECHNOLOGY","occurredOn":"%s"}
                """.formatted(lastMonth));
        postAs(u, "/api/transactions", """
                {"description":"Mesada","amount":300,"type":"INCOME","category":"ALLOWANCE","occurredOn":"%s"}
                """.formatted(today));
        postAs(u, "/api/transactions", """
                {"description":"Lanche","amount":20.5,"type":"EXPENSE","category":"FOOD","occurredOn":"%s"}
                """.formatted(today));
        long goal = readLong(postAs(u, "/api/goals", "{\"title\":\"Bike\",\"category\":\"LEISURE\",\"targetAmount\":400}"),
                "$.goal.id");
        postAs(u, "/api/goals/" + goal + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":100}");

        getAs(u, "/api/dashboard")
                .andExpect(jsonPath("$.goals.active").value(1))
                .andExpect(jsonPath("$.goals.totalSaved").value(100.0))
                .andExpect(jsonPath("$.goals.overallProgress").value(25.0))
                .andExpect(jsonPath("$.monthFinances.income").value(300.0))
                .andExpect(jsonPath("$.monthFinances.expense").value(20.5))
                .andExpect(jsonPath("$.hasTransactions").value(true))
                .andExpect(jsonPath("$.recentActivity[0].type").value("GOAL_MOVEMENT"))
                .andExpect(jsonPath("$.savingsEvolution.length()").value(6));

        String from = lastMonth.withDayOfMonth(1).toString();
        getAs(u, "/api/reports?from=" + from + "&to=" + today)
                .andExpect(jsonPath("$.finances.income").value(600.0))
                .andExpect(jsonPath("$.finances.expense").value(100.5))
                .andExpect(jsonPath("$.finances.monthly.length()").value(2))
                .andExpect(jsonPath("$.finances.monthly[0].month").value(YearMonth.from(lastMonth).toString()))
                .andExpect(jsonPath("$.finances.monthly[0].cumulativeBalance").value(220.0))
                .andExpect(jsonPath("$.finances.monthly[1].cumulativeBalance").value(499.5))
                .andExpect(jsonPath("$.goals.contributedInPeriod").value(100.0))
                .andExpect(jsonPath("$.goals.completionRate").value(0.0))
                .andExpect(jsonPath("$.criteria").isNotEmpty());

        // Período começando neste mês: o saldo de abertura inclui o mês anterior
        getAs(u, "/api/reports?from=" + today.withDayOfMonth(1) + "&to=" + today)
                .andExpect(jsonPath("$.finances.openingBalance").value(220.0))
                .andExpect(jsonPath("$.finances.income").value(300.0));
    }

    @Test
    void invalidPeriodsAreRejected() throws Exception {
        TestUser u = register();
        getAs(u, "/api/reports?from=2026-10-01&to=2026-01-01").andExpect(status().isUnprocessableEntity());
        getAs(u, "/api/reports?from=2010-01-01&to=2026-01-01").andExpect(status().isUnprocessableEntity());
        getAs(u, "/api/reports?from=ontem").andExpect(status().isBadRequest());
    }
}

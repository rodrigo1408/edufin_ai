package br.finedu.transaction;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionIntegrationTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now();

    private long tx(TestUser u, String desc, String amount, String type, String category, LocalDate date) throws Exception {
        return readLong(postAs(u, "/api/transactions", """
                {"description":"%s","amount":%s,"type":"%s","category":"%s","occurredOn":"%s"}
                """.formatted(desc, amount, type, category, date)).andExpect(status().isCreated()), "$.transaction.id");
    }

    @Test
    void createListFilterSortAndSummarize() throws Exception {
        TestUser u = register();
        LocalDate d = today.withDayOfMonth(1);
        tx(u, "Mesada", "200.00", "INCOME", "ALLOWANCE", d);
        tx(u, "Lanche", "15.30", "EXPENSE", "FOOD", d);
        tx(u, "Cinema", "30.00", "EXPENSE", "LEISURE", d);
        tx(u, "Lanche 2", "4.70", "EXPENSE", "FOOD", d);

        getAs(u, "/api/transactions?size=2&sort=AMOUNT&direction=DESC")
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(200.0))
                .andExpect(jsonPath("$.content[0].origin").value("MANUAL"));
        getAs(u, "/api/transactions?type=EXPENSE&category=FOOD")
                .andExpect(jsonPath("$.totalElements").value(2));
        getAs(u, "/api/transactions?q=cine").andExpect(jsonPath("$.content[0].description").value("Cinema"));
        getAs(u, "/api/transactions?from=" + today.plusDays(1)).andExpect(jsonPath("$.totalElements").value(0));

        getAs(u, "/api/transactions/summary?from=" + d + "&to=" + today)
                .andExpect(jsonPath("$.income").value(200.0))
                .andExpect(jsonPath("$.expense").value(50.0))
                .andExpect(jsonPath("$.balance").value(150.0))
                .andExpect(jsonPath("$.expensesByCategory[0].category").value("LEISURE"))
                .andExpect(jsonPath("$.expensesByCategory[1].total").value(20.0));
    }

    @Test
    void editAndDeleteOwnTransactions() throws Exception {
        TestUser u = register();
        long id = tx(u, "Uber", "20", "EXPENSE", "TRANSPORT", today);
        putAs(u, "/api/transactions/" + id, """
                {"description":"Ônibus","amount":5.50,"type":"EXPENSE","category":"TRANSPORT","occurredOn":"%s",
                 "origin":"SIMULATED","notes":"exercício"}
                """.formatted(today)).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5.5)).andExpect(jsonPath("$.origin").value("SIMULATED"));
        deleteAs(u, "/api/transactions/" + id).andExpect(status().isNoContent());
        getAs(u, "/api/transactions/" + id).andExpect(status().isNotFound());
    }

    @Test
    void validatesBusinessRules() throws Exception {
        TestUser u = register();
        postAs(u, "/api/transactions", """
                {"description":"x","amount":10,"type":"INCOME","category":"FOOD","occurredOn":"%s"}
                """.formatted(today)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CATEGORY_MISMATCH"));
        postAs(u, "/api/transactions", """
                {"description":"x","amount":10,"type":"EXPENSE","category":"FOOD","occurredOn":"%s","origin":"EXTERNAL"}
                """.formatted(today)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ORIGIN"));
        postAs(u, "/api/transactions", """
                {"description":"x","amount":10,"type":"EXPENSE","category":"FOOD","occurredOn":"%s"}
                """.formatted(today.plusDays(10))).andExpect(status().isUnprocessableEntity());
        postAs(u, "/api/transactions", """
                {"description":"x","amount":0.001,"type":"EXPENSE","category":"FOOD","occurredOn":"%s"}
                """.formatted(today)).andExpect(status().isBadRequest());
        getAs(u, "/api/transactions/summary?from=2026-05-01&to=2026-01-01").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void isolationBetweenUsers() throws Exception {
        TestUser owner = register();
        TestUser intruder = register();
        long id = tx(owner, "Segredo", "99", "EXPENSE", "OTHER_EXPENSE", today);
        getAs(intruder, "/api/transactions/" + id).andExpect(status().isNotFound());
        putAs(intruder, "/api/transactions/" + id, """
                {"description":"hack","amount":1,"type":"EXPENSE","category":"FOOD","occurredOn":"%s"}
                """.formatted(today)).andExpect(status().isNotFound());
        deleteAs(intruder, "/api/transactions/" + id).andExpect(status().isNotFound());
        getAs(intruder, "/api/transactions").andExpect(jsonPath("$.totalElements").value(0));
        getAs(intruder, "/api/transactions/summary").andExpect(jsonPath("$.expense").value(0.0));

        long goalId = readLong(postAs(owner, "/api/goals",
                "{\"title\":\"Meta\",\"category\":\"OTHER\",\"targetAmount\":10}"), "$.goal.id");
        postAs(intruder, "/api/transactions", """
                {"description":"x","amount":1,"type":"EXPENSE","category":"FOOD","occurredOn":"%s","goalId":%d}
                """.formatted(today, goalId)).andExpect(status().isNotFound());
    }
}

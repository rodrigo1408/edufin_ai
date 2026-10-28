package br.finedu.portfolio;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class WalletIntegrationTest extends IntegrationTest {

    private long wallet(TestUser u) throws Exception {
        return readLong(postAs(u, "/api/wallets", """
                {"name":"Carteira de estudo","assumedAnnualRate":12,"initialBalance":1000}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.wallet.wallet.simulated").value(true))
                .andExpect(jsonPath("$.wallet.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.wallet.wallet.balance").value(1000.0)), "$.wallet.wallet.id");
    }

    @Test
    void depositsWithdrawalsAndConsistentHistory() throws Exception {
        TestUser u = register();
        long id = wallet(u);
        postAs(u, "/api/wallets/" + id + "/movements", "{\"type\":\"DEPOSIT\",\"amount\":250.25}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.wallet.balance").value(1250.25));
        postAs(u, "/api/wallets/" + id + "/movements", "{\"type\":\"WITHDRAWAL\",\"amount\":5000}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
        postAs(u, "/api/wallets/" + id + "/movements", "{\"type\":\"WITHDRAWAL\",\"amount\":50.25}")
                .andExpect(jsonPath("$.wallet.balance").value(1200.0))
                .andExpect(jsonPath("$.performance.deposits").value(250.25))
                .andExpect(jsonPath("$.performance.withdrawals").value(50.25))
                .andExpect(jsonPath("$.performance.netContributed").value(1200.0));
        postAs(u, "/api/wallets/" + id + "/movements",
                "{\"type\":\"DEPOSIT\",\"amount\":10,\"occurredOn\":\"%s\"}".formatted(LocalDate.now().plusDays(3)))
                .andExpect(status().isUnprocessableEntity());
        getAs(u, "/api/wallets/" + id + "/movements").andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void simulatedReturnsOnlyForClosedMonthsAndOnce() throws Exception {
        TestUser u = register();
        long id = wallet(u);
        postAs(u, "/api/wallets/" + id + "/returns", "{\"period\":\"%s\"}".formatted(YearMonth.now()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
        postAs(u, "/api/wallets/" + id + "/returns", "{\"period\":\"2020-01\"}")
                .andExpect(status().isUnprocessableEntity());
        postAs(u, "/api/wallets/" + id + "/returns", "{\"period\":\"2026-13\"}").andExpect(status().isBadRequest());
    }

    @Test
    void scenariosCompareHypotheticalRates() throws Exception {
        TestUser u = register();
        long id = wallet(u);
        postAs(u, "/api/wallets/" + id + "/scenarios", """
                {"months":12,"monthlyContribution":0,"annualRates":[0,12]}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios[0].result.finalAmount").value(1000.0))
                .andExpect(jsonPath("$.scenarios[1].result.finalAmount").value(1120.0))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void archivedWalletIsReadOnlyAndWalletsArePrivate() throws Exception {
        TestUser u = register();
        TestUser other = register();
        long id = wallet(u);
        getAs(other, "/api/wallets/" + id).andExpect(status().isNotFound());
        postAs(other, "/api/wallets/" + id + "/movements", "{\"type\":\"DEPOSIT\",\"amount\":10}")
                .andExpect(status().isNotFound());
        postAs(u, "/api/wallets/" + id + "/archive", null).andExpect(jsonPath("$.wallet.archived").value(true));
        postAs(u, "/api/wallets/" + id + "/movements", "{\"type\":\"DEPOSIT\",\"amount\":10}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("WALLET_ARCHIVED"));
        deleteAs(u, "/api/wallets/" + id).andExpect(status().isNoContent());
        getAs(u, "/api/wallets").andExpect(jsonPath("$.length()").value(0));
    }
}

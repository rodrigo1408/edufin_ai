package br.finedu.calculator;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class CalculatorIntegrationTest extends IntegrationTest {

    private static final String INPUT = """
            {"interestType":"COMPOUND","principal":100,"ratePercent":10,"rateUnit":"MONTHLY","period":2,
             "periodUnit":"MONTHS","monthlyContribution":0}
            """;

    @Test
    void calculatesAndExplainsAssumptions() throws Exception {
        TestUser u = register();
        postAs(u, "/api/calculator/calculate", INPUT).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.finalAmount").value(121.0))
                .andExpect(jsonPath("$.result.series.length()").value(3))
                .andExpect(jsonPath("$.assumptions[0]").value("Simulação matemática: não é previsão nem garantia de rentabilidade."));
    }

    @Test
    void validatesInputs() throws Exception {
        TestUser u = register();
        postAs(u, "/api/calculator/calculate", INPUT.replace("\"ratePercent\":10", "\"ratePercent\":150"))
                .andExpect(status().isBadRequest());
        postAs(u, "/api/calculator/calculate", INPUT.replace("\"principal\":100", "\"principal\":-1"))
                .andExpect(status().isBadRequest());
        postAs(u, "/api/calculator/calculate", INPUT.replace("\"period\":2", "\"period\":0"))
                .andExpect(status().isBadRequest());
        String sixtyYears = INPUT.replace("\"period\":2", "\"period\":60")
                .replace("\"periodUnit\":\"MONTHS\"", "\"periodUnit\":\"YEARS\"");
        postAs(u, "/api/calculator/calculate", sixtyYears).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
        postAs(u, "/api/calculator/compare", "{\"scenarios\":[" + INPUT + "]}").andExpect(status().isBadRequest());
        postAs(u, "/api/calculator/compare", "{\"scenarios\":[" + INPUT + "," + INPUT.replace("COMPOUND", "SIMPLE") + "]}")
                .andExpect(jsonPath("$[1].result.finalAmount").value(120.0));
    }

    @Test
    void savedSimulationIsRecomputedByServerAndPrivate() throws Exception {
        TestUser u = register();
        TestUser other = register();
        long id = readLong(postAs(u, "/api/calculator/simulations",
                "{\"name\":\"Teste\",\"input\":" + INPUT + ",\"result\":{\"finalAmount\":999999}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulation.finalAmount").value(121.0)), "$.simulation.id");
        getAs(u, "/api/calculator/simulations").andExpect(jsonPath("$.length()").value(1));
        getAs(other, "/api/calculator/simulations/" + id).andExpect(status().isNotFound());
        deleteAs(other, "/api/calculator/simulations/" + id).andExpect(status().isNotFound());
        deleteAs(u, "/api/calculator/simulations/" + id).andExpect(status().isNoContent());
    }
}

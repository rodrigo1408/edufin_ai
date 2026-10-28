package br.finedu.calculator;

import br.finedu.calculator.CalculatorDtos.CalculationResponse;
import br.finedu.calculator.CalculatorDtos.InputDto;
import br.finedu.calculator.CalculatorDtos.SaveRequest;
import br.finedu.calculator.CalculatorDtos.SaveResponse;
import br.finedu.calculator.CalculatorDtos.SimulationView;
import br.finedu.calculator.InterestCalculator.Result;
import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculatorService {

    public static final List<String> ASSUMPTIONS = List.of(
            "Simulação matemática: não é previsão nem garantia de rentabilidade.",
            "Os aportes mensais são feitos no final de cada mês.",
            "A taxa é considerada constante durante todo o período.",
            "Juros compostos com taxa anual usam a taxa mensal equivalente: (1 + taxa anual)^(1/12) − 1.",
            "Juros simples com taxa anual usam a taxa mensal proporcional: taxa anual ÷ 12.",
            "Não são considerados impostos, tarifas nem inflação.");

    private static final int MAX_SAVED = 100;

    private final SimulationRepository simulations;
    private final GamificationService gamification;
    private final ActivityService activity;
    private final AppTime time;

    public CalculatorService(SimulationRepository simulations, GamificationService gamification,
                             ActivityService activity, AppTime time) {
        this.simulations = simulations;
        this.gamification = gamification;
        this.activity = activity;
        this.time = time;
    }

    public CalculationResponse calculate(InputDto input) {
        return new CalculationResponse(input, compute(input), ASSUMPTIONS);
    }

    public Result compute(InputDto in) {
        int months = InterestCalculator.toMonths(in.period(), in.periodUnit());
        if (months > InterestCalculator.MAX_MONTHS) {
            throw ApiException.business("INVALID_PERIOD", "O período máximo é de 50 anos (600 meses).");
        }
        return InterestCalculator.calculate(in.toInput());
    }

    @Transactional(readOnly = true)
    public List<SimulationView> list(Long userId) {
        return simulations.findByUserIdOrderByCreatedAtDesc(userId).stream().map(CalculatorService::view).toList();
    }

    @Transactional(readOnly = true)
    public CalculationResponse get(Long userId, Long id) {
        Simulation s = owned(userId, id);
        return calculate(InputDto.of(s.toInput()));
    }

    /** O resultado salvo é sempre recalculado no servidor; o cliente envia apenas os parâmetros. */
    @Transactional
    public SaveResponse save(Long userId, SaveRequest req) {
        if (simulations.countByUserId(userId) >= MAX_SAVED) {
            throw ApiException.business("LIMIT_REACHED", "Limite de 100 simulações salvas. Exclua algumas antigas.");
        }
        Result result = compute(req.input());
        Simulation s = simulations.save(new Simulation(userId, req.name().trim(), req.input().toInput(), result,
                time.now()));
        activity.record(userId, ActivityLog.Type.SIMULATION_SAVED, "Simulação salva: " + s.getName());
        Reward reward = gamification.awardDaily(userId, PointRule.SIMULATION_SAVED);
        return new SaveResponse(view(s), reward);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        simulations.delete(owned(userId, id));
    }

    private Simulation owned(Long userId, Long id) {
        return simulations.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Simulação"));
    }

    private static SimulationView view(Simulation s) {
        return new SimulationView(s.getId(), s.getName(), InputDto.of(s.toInput()), s.getFinalAmount(),
                s.getTotalInvested(), s.getTotalInterest(), s.getCreatedAt());
    }
}

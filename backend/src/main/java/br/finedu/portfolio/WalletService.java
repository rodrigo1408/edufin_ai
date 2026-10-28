package br.finedu.portfolio;

import br.finedu.calculator.InterestCalculator;
import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.Money;
import br.finedu.common.PageResponse;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.portfolio.WalletDtos.BalancePoint;
import br.finedu.portfolio.WalletDtos.CreateResult;
import br.finedu.portfolio.WalletDtos.MovementRequest;
import br.finedu.portfolio.WalletDtos.MovementView;
import br.finedu.portfolio.WalletDtos.Performance;
import br.finedu.portfolio.WalletDtos.ReturnRequest;
import br.finedu.portfolio.WalletDtos.ScenarioRequest;
import br.finedu.portfolio.WalletDtos.ScenarioResponse;
import br.finedu.portfolio.WalletDtos.ScenarioResult;
import br.finedu.portfolio.WalletDtos.WalletDetail;
import br.finedu.portfolio.WalletDtos.WalletRequest;
import br.finedu.portfolio.WalletDtos.WalletSummary;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carteiras simuladas. O saldo é sempre derivado do histórico de movimentações; a carteira é
 * bloqueada durante cada movimentação para manter o histórico consistente.
 */
@Service
public class WalletService {

    private static final int MAX_WALLETS = 10;

    private final WalletRepository wallets;
    private final WalletMovementRepository movements;
    private final GamificationService gamification;
    private final ActivityService activity;
    private final AppTime time;

    public WalletService(WalletRepository wallets, WalletMovementRepository movements, GamificationService gamification,
                         ActivityService activity, AppTime time) {
        this.wallets = wallets;
        this.movements = movements;
        this.gamification = gamification;
        this.activity = activity;
        this.time = time;
    }

    @Transactional
    public CreateResult create(Long userId, WalletRequest req) {
        if (wallets.countByUserId(userId) >= MAX_WALLETS) {
            throw ApiException.business("LIMIT_REACHED", "Limite de " + MAX_WALLETS + " carteiras simuladas.");
        }
        Wallet w = wallets.save(new Wallet(userId, req.name().trim(), blankToNull(req.description()),
                req.assumedAnnualRate(), time.now()));
        BigDecimal initial = req.initialBalance() == null ? Money.zero() : Money.of(req.initialBalance());
        movements.save(new WalletMovement(w.getId(), WalletMovement.Type.INITIAL, initial, time.today(),
                "Saldo inicial simulado", null, time.now()));
        activity.record(userId, ActivityLog.Type.WALLET_CREATED, "Carteira simulada criada: " + w.getName());
        Reward reward = gamification.awardDaily(userId, PointRule.WALLET_CREATED);
        return new CreateResult(detail(userId, w.getId()), reward);
    }

    @Transactional
    public WalletDetail update(Long userId, Long walletId, WalletRequest req) {
        Wallet w = owned(userId, walletId);
        w.update(req.name().trim(), blankToNull(req.description()), req.assumedAnnualRate());
        return detail(userId, walletId);
    }

    @Transactional
    public WalletDetail setArchived(Long userId, Long walletId, boolean archived) {
        owned(userId, walletId).setArchived(archived);
        return detail(userId, walletId);
    }

    @Transactional
    public void delete(Long userId, Long walletId) {
        wallets.delete(owned(userId, walletId));
    }

    @Transactional(readOnly = true)
    public List<WalletSummary> list(Long userId) {
        return wallets.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> summary(w, balance(movements.findByWalletIdOrderByOccurredOnAscIdAsc(w.getId())))).toList();
    }

    @Transactional(readOnly = true)
    public WalletDetail detail(Long userId, Long walletId) {
        Wallet w = owned(userId, walletId);
        List<WalletMovement> list = movements.findByWalletIdOrderByOccurredOnAscIdAsc(walletId);
        return new WalletDetail(summary(w, balance(list)), performance(list), evolution(list), WalletDtos.DISCLAIMER);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementView> movements(Long userId, Long walletId, int page, int size) {
        owned(userId, walletId);
        return PageResponse.of(movements.findByWalletIdOrderByOccurredOnDescIdDesc(walletId, PageRequest.of(page, size)),
                MovementView::of);
    }

    @Transactional
    public WalletDetail addMovement(Long userId, Long walletId, MovementRequest req) {
        Wallet w = wallets.lockByIdAndUserId(walletId, userId).orElseThrow(() -> ApiException.notFound("Carteira"));
        requireActive(w);
        LocalDate date = req.occurredOn() == null ? time.today() : req.occurredOn();
        LocalDate created = time.toDate(w.getCreatedAt());
        if (date.isAfter(time.today()) || date.isBefore(created)) {
            throw ApiException.business("INVALID_DATE", "A data deve estar entre a criação da carteira e hoje.");
        }
        BigDecimal amount = Money.requirePositive(req.amount(), "O valor");
        List<WalletMovement> history = movements.findByWalletIdOrderByOccurredOnAscIdAsc(walletId);
        WalletMovement.Type type = req.type() == WalletDtos.ManualType.DEPOSIT
                ? WalletMovement.Type.DEPOSIT : WalletMovement.Type.WITHDRAWAL;
        if (type == WalletMovement.Type.WITHDRAWAL) {
            LocalDate last = history.isEmpty() ? created : history.get(history.size() - 1).getOccurredOn();
            if (date.isBefore(last)) {
                throw ApiException.business("INVALID_DATE",
                        "Retiradas precisam ter data igual ou posterior à última movimentação (" + last + ").");
            }
            if (amount.compareTo(balance(history)) > 0) {
                throw ApiException.business("INSUFFICIENT_BALANCE", "Saldo simulado insuficiente para esta retirada.");
            }
        }
        movements.save(new WalletMovement(walletId, type, amount, date, blankToNull(req.note()), null, time.now()));
        activity.record(userId, ActivityLog.Type.WALLET_MOVEMENT,
                (type == WalletMovement.Type.DEPOSIT ? "Aporte" : "Retirada") + " simulado(a) na carteira " + w.getName());
        return detail(userId, walletId);
    }

    /**
     * Aplica o rendimento simulado de um mês já encerrado: saldo ao fim do mês × taxa mensal
     * equivalente à taxa anual hipotética da carteira. Cada mês só pode ser aplicado uma vez.
     */
    @Transactional
    public WalletDetail applyMonthlyReturn(Long userId, Long walletId, ReturnRequest req) {
        Wallet w = wallets.lockByIdAndUserId(walletId, userId).orElseThrow(() -> ApiException.notFound("Carteira"));
        requireActive(w);
        YearMonth period = YearMonth.parse(req.period());
        YearMonth createdMonth = YearMonth.from(time.toDate(w.getCreatedAt()));
        if (!period.isBefore(YearMonth.from(time.today()))) {
            throw ApiException.business("INVALID_PERIOD", "Só é possível aplicar rendimentos de meses já encerrados.");
        }
        if (period.isBefore(createdMonth)) {
            throw ApiException.business("INVALID_PERIOD", "O mês é anterior à criação da carteira.");
        }
        if (movements.existsByWalletIdAndReferencePeriod(walletId, req.period())) {
            throw ApiException.conflict("RETURN_ALREADY_APPLIED", "O rendimento simulado deste mês já foi aplicado.");
        }
        LocalDate monthEnd = period.atEndOfMonth();
        BigDecimal base = movements.findByWalletIdOrderByOccurredOnAscIdAsc(walletId).stream()
                .filter(m -> !m.getOccurredOn().isAfter(monthEnd))
                .map(WalletMovement::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rate = InterestCalculator.monthlyRate(InterestCalculator.InterestType.COMPOUND,
                w.getAssumedAnnualRate(), InterestCalculator.RateUnit.YEARLY);
        BigDecimal amount = base.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
        if (amount.signum() <= 0) {
            throw ApiException.business("NO_RETURN", "Não há saldo ou taxa suficiente para gerar rendimento neste mês.");
        }
        movements.save(new WalletMovement(walletId, WalletMovement.Type.SIMULATED_RETURN, amount, monthEnd,
                "Rendimento simulado de " + req.period() + " (taxa hipotética de " + w.getAssumedAnnualRate() + "% a.a.)",
                req.period(), time.now()));
        return detail(userId, walletId);
    }

    @Transactional(readOnly = true)
    public ScenarioResponse scenarios(Long userId, Long walletId, ScenarioRequest req) {
        owned(userId, walletId);
        BigDecimal start = balance(movements.findByWalletIdOrderByOccurredOnAscIdAsc(walletId));
        List<ScenarioResult> results = req.annualRates().stream().map(rate -> new ScenarioResult(rate,
                InterestCalculator.calculate(new InterestCalculator.Input(InterestCalculator.InterestType.COMPOUND,
                        start, rate, InterestCalculator.RateUnit.YEARLY, req.months(),
                        InterestCalculator.PeriodUnit.MONTHS, req.monthlyContribution())))).toList();
        return new ScenarioResponse(start, req.months(), req.monthlyContribution(), results, WalletDtos.DISCLAIMER);
    }

    static BigDecimal balance(List<WalletMovement> list) {
        return Money.of(list.stream().map(WalletMovement::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    static Performance performance(List<WalletMovement> list) {
        BigDecimal initial = sum(list, WalletMovement.Type.INITIAL);
        BigDecimal deposits = sum(list, WalletMovement.Type.DEPOSIT);
        BigDecimal withdrawals = sum(list, WalletMovement.Type.WITHDRAWAL);
        BigDecimal returns = sum(list, WalletMovement.Type.SIMULATED_RETURN);
        BigDecimal net = initial.add(deposits).subtract(withdrawals);
        BigDecimal pct = net.signum() <= 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : returns.multiply(BigDecimal.valueOf(100)).divide(net, 2, RoundingMode.HALF_EVEN);
        return new Performance(initial, deposits, withdrawals, returns, Money.of(net), balance(list), pct);
    }

    /** Saldo acumulado ao final de cada dia com movimentação. */
    static List<BalancePoint> evolution(List<WalletMovement> list) {
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        list.forEach(m -> byDay.merge(m.getOccurredOn(), m.signedAmount(), BigDecimal::add));
        List<BalancePoint> points = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, BigDecimal> e : byDay.entrySet()) {
            running = running.add(e.getValue());
            points.add(new BalancePoint(e.getKey(), Money.of(running)));
        }
        return points;
    }

    private static BigDecimal sum(List<WalletMovement> list, WalletMovement.Type type) {
        return Money.of(list.stream().filter(m -> m.getType() == type).map(WalletMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Wallet owned(Long userId, Long walletId) {
        return wallets.findByIdAndUserId(walletId, userId).orElseThrow(() -> ApiException.notFound("Carteira"));
    }

    private static void requireActive(Wallet w) {
        if (w.isArchived()) {
            throw ApiException.business("WALLET_ARCHIVED", "Carteira arquivada. Reative-a para registrar movimentações.");
        }
    }

    private static WalletSummary summary(Wallet w, BigDecimal balance) {
        return new WalletSummary(w.getId(), w.getName(), w.getDescription(), w.getAssumedAnnualRate(), balance,
                w.isArchived(), w.getCreatedAt(), true);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

package br.finedu.transaction;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.Money;
import br.finedu.common.PageResponse;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.goal.GoalService;
import br.finedu.transaction.TransactionDtos.CategoryTotal;
import br.finedu.transaction.TransactionDtos.Summary;
import br.finedu.transaction.TransactionDtos.TransactionRequest;
import br.finedu.transaction.TransactionDtos.TransactionResult;
import br.finedu.transaction.TransactionDtos.TransactionView;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    public record Filter(LocalDate from, LocalDate to, Transaction.Type type, TransactionCategory category,
                         Transaction.Origin origin, String q) {
    }

    public enum SortField { DATE, AMOUNT, DESCRIPTION }

    private final TransactionRepository repository;
    private final GoalService goals;
    private final GamificationService gamification;
    private final ActivityService activity;
    private final AppTime time;

    public TransactionService(TransactionRepository repository, GoalService goals, GamificationService gamification,
                              ActivityService activity, AppTime time) {
        this.repository = repository;
        this.goals = goals;
        this.gamification = gamification;
        this.activity = activity;
        this.time = time;
    }

    @Transactional
    public TransactionResult create(Long userId, TransactionRequest req) {
        Transaction t = new Transaction(userId, Transaction.Origin.MANUAL, time.now());
        apply(userId, t, req);
        repository.save(t);
        activity.record(userId, ActivityLog.Type.TRANSACTION_CREATED, "Movimentação registrada: " + t.getDescription());
        Reward reward = gamification.awardDaily(userId, PointRule.TRANSACTION_LOGGED);
        return new TransactionResult(TransactionView.of(t), reward);
    }

    @Transactional
    public TransactionView update(Long userId, Long id, TransactionRequest req) {
        Transaction t = owned(userId, id);
        requireEditable(t);
        apply(userId, t, req);
        activity.record(userId, ActivityLog.Type.TRANSACTION_UPDATED, "Movimentação editada: " + t.getDescription());
        return TransactionView.of(t);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Transaction t = owned(userId, id);
        requireEditable(t);
        repository.delete(t);
        activity.record(userId, ActivityLog.Type.TRANSACTION_DELETED, "Movimentação excluída: " + t.getDescription());
    }

    @Transactional(readOnly = true)
    public TransactionView get(Long userId, Long id) {
        return TransactionView.of(owned(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionView> list(Long userId, Filter filter, SortField sortField, Sort.Direction dir,
                                              int page, int size) {
        String prop = switch (sortField) {
            case DATE -> "occurredOn";
            case AMOUNT -> "amount";
            case DESCRIPTION -> "description";
        };
        Sort sort = Sort.by(dir, prop).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(repository.findAll(spec(userId, filter), PageRequest.of(page, size, sort)),
                TransactionView::of);
    }

    /** Receitas, despesas e saldo do período (padrão: mês corrente). */
    @Transactional(readOnly = true)
    public Summary summary(Long userId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : time.today().withDayOfMonth(1);
        LocalDate end = to != null ? to : time.today();
        validatePeriod(start, end);
        BigDecimal income = Money.zero();
        BigDecimal expense = Money.zero();
        for (Object[] row : repository.totalsByType(userId, start, end)) {
            if (row[0] == Transaction.Type.INCOME) {
                income = Money.of((BigDecimal) row[1]);
            } else {
                expense = Money.of((BigDecimal) row[1]);
            }
        }
        return new Summary(start, end, income, expense, income.subtract(expense),
                categoryTotals(userId, Transaction.Type.EXPENSE, start, end),
                categoryTotals(userId, Transaction.Type.INCOME, start, end));
    }

    public List<CategoryTotal> categoryTotals(Long userId, Transaction.Type type, LocalDate from, LocalDate to) {
        List<CategoryTotal> list = new ArrayList<>();
        for (Object[] row : repository.totalsByCategory(userId, type, from, to)) {
            TransactionCategory c = (TransactionCategory) row[0];
            list.add(new CategoryTotal(c, c.label(), Money.of((BigDecimal) row[1]), (Long) row[2]));
        }
        list.sort(Comparator.comparing(CategoryTotal::total).reversed());
        return list;
    }

    public static void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw ApiException.business("INVALID_PERIOD", "A data inicial deve ser anterior ou igual à final.");
        }
        if (from.plusYears(5).isBefore(to)) {
            throw ApiException.business("INVALID_PERIOD", "O período máximo é de 5 anos.");
        }
    }

    private void apply(Long userId, Transaction t, TransactionRequest req) {
        if (req.category().type() != req.type()) {
            throw ApiException.business("CATEGORY_MISMATCH", "A categoria escolhida não corresponde ao tipo da movimentação.");
        }
        Transaction.Origin origin = req.origin() == null ? Transaction.Origin.MANUAL : req.origin();
        if (origin == Transaction.Origin.EXTERNAL) {
            throw ApiException.business("INVALID_ORIGIN", "Registros de integrações externas não podem ser criados manualmente.");
        }
        if (req.occurredOn().isAfter(time.today().plusDays(1))) {
            throw ApiException.business("FUTURE_DATE", "A data da movimentação não pode estar no futuro.");
        }
        if (req.occurredOn().isBefore(time.today().minusYears(10))) {
            throw ApiException.business("INVALID_DATE", "A data é antiga demais.");
        }
        if (req.goalId() != null) {
            goals.requireOwned(userId, req.goalId());
        }
        String notes = req.notes() == null || req.notes().isBlank() ? null : req.notes().trim();
        t.apply(req.description().trim(), Money.requirePositive(req.amount(), "O valor"), req.type(), req.category(),
                req.occurredOn(), notes, req.goalId(), origin, time.now());
    }

    private Transaction owned(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Movimentação"));
    }

    private static void requireEditable(Transaction t) {
        if (t.getOrigin() == Transaction.Origin.EXTERNAL) {
            throw ApiException.business("READ_ONLY", "Registros importados de integrações não podem ser alterados.");
        }
    }

    static Specification<Transaction> spec(Long userId, Filter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("occurredOn"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("occurredOn"), f.to()));
            }
            if (f.type() != null) {
                p.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.category() != null) {
                p.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.origin() != null) {
                p.add(cb.equal(root.get("origin"), f.origin()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                p.add(cb.like(cb.lower(root.get("description")), "%" + escapeLike(f.q().trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

package br.finedu.admin;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.PageResponse;
import br.finedu.security.Role;
import br.finedu.transaction.TransactionService;
import br.finedu.user.PasswordResetService;
import br.finedu.user.User;
import br.finedu.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de contas (permissão USER_MANAGE). Administradores NÃO têm acesso aos dados financeiros
 * dos usuários: só a dados de conta necessários para suporte.
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    public record AdminUserView(Long id, String email, String displayName, Role role, boolean enabled,
                                Instant createdAt, Instant lastLoginAt) {
        static AdminUserView of(User u) {
            return new AdminUserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.isEnabled(),
                    u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    private final UserRepository users;
    private final PasswordResetService passwordReset;
    private final AppTime time;

    public AdminUserService(UserRepository users, PasswordResetService passwordReset, AppTime time) {
        this.users = users;
        this.passwordReset = passwordReset;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserView> search(String q, Role role, int page, int size) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(users.findAll(spec(term, role),
                PageRequest.of(page, size, Sort.by("createdAt").descending())), AdminUserView::of);
    }

    /** Altera o papel. Impede alterar o próprio papel e remover o último administrador ativo. */
    @Transactional
    public AdminUserView changeRole(Long adminId, Long userId, Role role) {
        if (adminId.equals(userId)) {
            throw ApiException.business("SELF_CHANGE", "Você não pode alterar o seu próprio papel.");
        }
        User u = get(userId);
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN && u.isEnabled()
                && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.business("LAST_ADMIN", "É necessário manter ao menos um administrador ativo.");
        }
        Role previous = u.getRole();
        u.changeRole(role, time.now());
        log.info("Papel alterado: usuário={} de={} para={} por admin={}", userId, previous, role, adminId);
        return AdminUserView.of(u);
    }

    @Transactional
    public AdminUserView setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId)) {
            throw ApiException.business("SELF_CHANGE", "Você não pode desativar a sua própria conta.");
        }
        User u = get(userId);
        if (!enabled && u.getRole() == Role.ADMIN && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.business("LAST_ADMIN", "É necessário manter ao menos um administrador ativo.");
        }
        u.setEnabled(enabled, time.now());
        log.info("Conta {}: usuário={} por admin={}", enabled ? "ativada" : "desativada", userId, adminId);
        return AdminUserView.of(u);
    }

    @Transactional
    public String resetLink(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw ApiException.business("SELF_CHANGE", "Use a opção de trocar senha no seu perfil.");
        }
        return passwordReset.issueLinkByAdmin(userId);
    }

    private User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuário"));
    }

    private static Specification<User> spec(String q, Role role) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null) {
                String like = "%" + TransactionService.escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("displayName")), like, '\\')));
            }
            if (role != null) {
                p.add(cb.equal(root.get("role"), role));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}

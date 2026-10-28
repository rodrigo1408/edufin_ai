package br.finedu.user;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.PageResponse;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.security.Role;
import br.finedu.user.UserDtos.ActivityView;
import br.finedu.user.UserDtos.MeResponse;
import br.finedu.user.UserDtos.PreferencesRequest;
import br.finedu.user.UserDtos.PreferencesResponse;
import br.finedu.user.UserDtos.RegisterRequest;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final ActivityLogRepository activityLogs;
    private final ActivityService activity;
    private final NotificationService notifications;
    private final PasswordEncoder passwordEncoder;
    private final AppTime time;

    public UserService(UserRepository users, UserPreferencesRepository preferences, ActivityLogRepository activityLogs,
                       ActivityService activity, NotificationService notifications, PasswordEncoder passwordEncoder,
                       AppTime time) {
        this.users = users;
        this.preferences = preferences;
        this.activityLogs = activityLogs;
        this.activity = activity;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.time = time;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User register(RegisterRequest req) {
        return createUser(req.email(), req.password(), req.displayName(), Role.USER);
    }

    @Transactional
    public User createUser(String rawEmail, String rawPassword, String rawDisplayName, Role role) {
        String email = normalizeEmail(rawEmail);
        String displayName = rawDisplayName.trim();
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_IN_USE", "Este e-mail já está cadastrado.");
        }
        if (users.existsByDisplayNameIgnoreCase(displayName)) {
            throw ApiException.conflict("DISPLAY_NAME_IN_USE", "Este apelido já está em uso. Escolha outro.");
        }
        User user = users.save(new User(email, passwordEncoder.encode(rawPassword), displayName, role, time.now()));
        preferences.save(new UserPreferences(user.getId()));
        activity.record(user.getId(), ActivityLog.Type.ACCOUNT_CREATED, "Conta criada");
        notifications.notify(user.getId(), NotificationType.WELCOME, "Boas-vindas ao EduFin AI!",
                "Comece criando sua primeira meta ou fazendo o curso \"Primeiros passos com o dinheiro\".",
                "/cursos", "WELCOME");
        log.info("Usuário criado id={} papel={}", user.getId(), role);
        return user;
    }

    @Transactional
    public void recordLogin(Long userId) {
        User user = get(userId);
        user.markLogin(time.now());
        activity.record(userId, ActivityLog.Type.LOGIN, "Acesso à plataforma");
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User u = get(userId);
        return new MeResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getRole().permissions(),
                u.getCreatedAt(), u.getLastLoginAt(), PreferencesResponse.of(preferences.getOrDefault(userId)));
    }

    @Transactional
    public MeResponse updateProfile(Long userId, String displayName) {
        User user = get(userId);
        String name = displayName.trim();
        if (!name.equalsIgnoreCase(user.getDisplayName()) && users.existsByDisplayNameIgnoreCase(name)) {
            throw ApiException.conflict("DISPLAY_NAME_IN_USE", "Este apelido já está em uso. Escolha outro.");
        }
        user.rename(name, time.now());
        activity.record(userId, ActivityLog.Type.PROFILE_UPDATED, "Apelido atualizado");
        return me(userId);
    }

    @Transactional
    public MeResponse changeEmail(Long userId, String newEmail, String currentPassword) {
        User user = get(userId);
        requirePassword(user, currentPassword);
        String email = normalizeEmail(newEmail);
        if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_IN_USE", "Este e-mail já está cadastrado.");
        }
        user.changeEmail(email, time.now());
        activity.record(userId, ActivityLog.Type.PROFILE_UPDATED, "E-mail atualizado");
        return me(userId);
    }

    /** Troca a senha e renova o carimbo de segurança (outras sessões abertas são encerradas). */
    @Transactional
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        User user = get(userId);
        requirePassword(user, currentPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw ApiException.business("SAME_PASSWORD", "A nova senha deve ser diferente da atual.");
        }
        user.changePassword(passwordEncoder.encode(newPassword), time.now());
        activity.record(userId, ActivityLog.Type.PASSWORD_CHANGED, "Senha alterada");
        return user;
    }

    @Transactional
    public PreferencesResponse updatePreferences(Long userId, PreferencesRequest req) {
        UserPreferences p = preferences.getOrDefault(userId);
        p.update(req.theme(), req.notifyGoals(), req.notifyLearning(), req.notifyAchievements(),
                req.notifyCommunity(), req.weeklyLessonTarget());
        preferences.save(p);
        activity.record(userId, ActivityLog.Type.PREFERENCES_UPDATED, "Preferências atualizadas");
        return PreferencesResponse.of(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityView> activity(Long userId, int page, int size) {
        return PageResponse.of(activityLogs.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
                ActivityView::of);
    }

    /**
     * Exclusão definitiva da conta (direito de eliminação — LGPD art. 18). Os dados pessoais são
     * removidos em cascata pelo banco; feedbacks permanecem anonimizados.
     */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = get(userId);
        requirePassword(user, password);
        if (user.getRole() == Role.ADMIN && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.business("LAST_ADMIN", "Não é possível excluir o último administrador.");
        }
        users.delete(user);
        log.info("Conta excluída id={}", userId);
    }

    public User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuário"));
    }

    private void requirePassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.business("WRONG_PASSWORD", "Senha atual incorreta.");
        }
    }
}

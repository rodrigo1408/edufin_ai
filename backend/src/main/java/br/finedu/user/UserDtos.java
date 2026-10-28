package br.finedu.user;

import br.finedu.security.Permission;
import br.finedu.security.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

/** Contratos de entrada/saída do módulo de usuários. Nunca incluem hash de senha ou tokens. */
public final class UserDtos {

    /** Senha: 8 a 72 caracteres (limite do BCrypt), com ao menos uma letra e um número. */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE = "A senha deve ter de 8 a 72 caracteres, com letras e números.";
    /** Apelido público: evita nome completo e caracteres especiais. */
    public static final String DISPLAY_NAME_REGEX = "^[\\p{L}\\p{N}_. -]{3,30}$";
    public static final String DISPLAY_NAME_MESSAGE =
            "O apelido deve ter de 3 a 30 caracteres (letras, números, espaço, ponto, hífen ou _).";

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank @Pattern(regexp = DISPLAY_NAME_REGEX, message = DISPLAY_NAME_MESSAGE) String displayName,
            @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade.")
            boolean acceptTerms,
            @AssertTrue(message = "Confirme a faixa etária e a ciência do(a) responsável, se aplicável.")
            boolean ageConfirmation) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Pattern(regexp = DISPLAY_NAME_REGEX, message = DISPLAY_NAME_MESSAGE) String displayName) {
    }

    public record ChangeEmailRequest(@NotBlank @Email @Size(max = 254) String email,
                                     @NotBlank @Size(max = 72) String currentPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record DeleteAccountRequest(@NotBlank @Size(max = 72) String password) {
    }

    public record PasswordResetRequest(@NotBlank @Email String email) {
    }

    public record PasswordResetConfirm(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record PreferencesRequest(
            @NotNull UserPreferences.Theme theme,
            boolean notifyGoals,
            boolean notifyLearning,
            boolean notifyAchievements,
            boolean notifyCommunity,
            @Min(1) @Max(50) int weeklyLessonTarget) {
    }

    public record PreferencesResponse(UserPreferences.Theme theme, boolean notifyGoals, boolean notifyLearning,
                                      boolean notifyAchievements, boolean notifyCommunity, int weeklyLessonTarget) {
        public static PreferencesResponse of(UserPreferences p) {
            return new PreferencesResponse(p.getTheme(), p.isNotifyGoals(), p.isNotifyLearning(),
                    p.isNotifyAchievements(), p.isNotifyCommunity(), p.getWeeklyLessonTarget());
        }
    }

    public record MeResponse(Long id, String email, String displayName, Role role, Set<Permission> permissions,
                             Instant createdAt, Instant lastLoginAt, PreferencesResponse preferences) {
    }

    public record ActivityView(Long id, ActivityLog.Type type, String description, Instant createdAt) {
        public static ActivityView of(ActivityLog a) {
            return new ActivityView(a.getId(), a.getType(), a.getDescription(), a.getCreatedAt());
        }
    }
}

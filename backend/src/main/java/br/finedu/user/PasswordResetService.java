package br.finedu.user;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperação de senha por token de uso único (30 min). Disponível somente quando existe um
 * {@link PasswordResetMailer} real configurado.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TTL = Duration.ofMinutes(30);

    private final ObjectProvider<PasswordResetMailer> mailer;
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ActivityService activity;
    private final AppTime time;
    private final String publicUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(ObjectProvider<PasswordResetMailer> mailer, UserRepository users,
                                PasswordResetTokenRepository tokens, PasswordEncoder passwordEncoder,
                                ActivityService activity, AppTime time, @Value("${app.public-url}") String publicUrl) {
        this.mailer = mailer;
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.activity = activity;
        this.time = time;
        this.publicUrl = publicUrl;
    }

    public boolean isAvailable() {
        return mailer.getIfAvailable() != null;
    }

    /**
     * Sempre responde da mesma forma, exista ou não a conta, para não permitir descobrir e-mails
     * cadastrados.
     */
    @Transactional
    public void request(String rawEmail) {
        PasswordResetMailer m = mailer.getIfAvailable();
        if (m == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RESET_UNAVAILABLE",
                    "A recuperação de senha por e-mail não está configurada nesta instalação. "
                            + "Peça ajuda a um(a) administrador(a) da plataforma.");
        }
        Optional<User> user = users.findByEmail(UserService.normalizeEmail(rawEmail));
        if (user.isEmpty() || !user.get().isEnabled()) {
            return;
        }
        m.sendResetLink(user.get().getEmail(), user.get().getDisplayName(), issueLink(user.get().getId()));
        log.info("Link de redefinição enviado para usuário id={}", user.get().getId());
    }

    /**
     * Alternativa sem e-mail: um administrador gera um link de uso único (30 min) e o entrega à
     * pessoa por um canal confiável. O link permite apenas definir uma nova senha.
     */
    @Transactional
    public String issueLinkByAdmin(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuário"));
        log.info("Link de redefinição gerado por administrador para usuário id={}", user.getId());
        return issueLink(user.getId());
    }

    private String issueLink(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.invalidateAll(userId, time.now());
        tokens.save(new PasswordResetToken(userId, sha256(token), time.now().plus(TTL), time.now()));
        return publicUrl + "/redefinir-senha?token=" + token;
    }

    @Transactional
    public void confirm(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(sha256(token))
                .filter(x -> x.isUsable(time.now()))
                .orElseThrow(() -> ApiException.business("INVALID_TOKEN", "Link inválido ou expirado. Solicite um novo."));
        User user = users.findById(t.getUserId()).orElseThrow(() -> ApiException.notFound("Usuário"));
        user.changePassword(passwordEncoder.encode(newPassword), time.now());
        tokens.invalidateAll(user.getId(), time.now());
        activity.record(user.getId(), ActivityLog.Type.PASSWORD_RESET, "Senha redefinida por link de recuperação");
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

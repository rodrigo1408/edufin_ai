package br.finedu.user;

import br.finedu.common.RateLimiter;
import br.finedu.security.AuthUser;
import br.finedu.security.SessionService;
import br.finedu.user.UserDtos.LoginRequest;
import br.finedu.user.UserDtos.MeResponse;
import br.finedu.user.UserDtos.PasswordResetConfirm;
import br.finedu.user.UserDtos.PasswordResetRequest;
import br.finedu.user.UserDtos.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionService sessions;
    private final UserService userService;
    private final PasswordResetService passwordReset;
    private final RateLimiter rateLimiter;

    private final int registrationsPerHourPerIp;

    public AuthController(AuthenticationManager authenticationManager, SessionService sessions,
                          UserService userService, PasswordResetService passwordReset, RateLimiter rateLimiter,
                          @Value("${app.rate-limit.registrations-per-hour-per-ip:30}") int registrationsPerHourPerIp) {
        this.authenticationManager = authenticationManager;
        this.sessions = sessions;
        this.userService = userService;
        this.passwordReset = passwordReset;
        this.rateLimiter = rateLimiter;
        this.registrationsPerHourPerIp = registrationsPerHourPerIp;
    }

    /** Garante que o cookie XSRF-TOKEN seja emitido antes do primeiro POST do SPA. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        // O token é carregado sob demanda; acessá-lo faz o repositório gravar o cookie XSRF-TOKEN.
        token.getToken();
        return Map.of("headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request,
                                               HttpServletResponse response) {
        // Limite por IP generoso: turmas inteiras costumam compartilhar o mesmo IP (rede da escola).
        rateLimiter.check("register:" + request.getRemoteAddr(), registrationsPerHourPerIp, Duration.ofHours(1));
        User user = userService.register(req);
        sessions.establish(user, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.me(user.getId()));
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
                            HttpServletResponse response) {
        rateLimiter.check("login-ip:" + request.getRemoteAddr(), 30, Duration.ofMinutes(5));
        rateLimiter.check("login-email:" + UserService.normalizeEmail(req.email()), 5, Duration.ofMinutes(5));
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(UserService.normalizeEmail(req.email()),
                        req.password()));
        sessions.establish(auth, request, response);
        Long userId = ((AuthUser) auth.getPrincipal()).id();
        userService.recordLogin(userId);
        return userService.me(userId);
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestReset(@Valid @RequestBody PasswordResetRequest req,
                                                            HttpServletRequest request) {
        rateLimiter.check("reset-ip:" + request.getRemoteAddr(), 5, Duration.ofMinutes(15));
        passwordReset.request(req.email());
        return ResponseEntity.accepted().body(Map.of("message",
                "Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha."));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirm req, HttpServletRequest request) {
        rateLimiter.check("reset-confirm-ip:" + request.getRemoteAddr(), 10, Duration.ofMinutes(15));
        passwordReset.confirm(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}

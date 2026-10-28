package br.finedu.security;

import br.finedu.user.User;
import br.finedu.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revalida a sessão a cada requisição: se a conta foi desativada, excluída, teve o papel alterado
 * ou a senha trocada (carimbo de segurança diferente), a sessão é encerrada imediatamente.
 */
public class SessionUserRefreshFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public SessionUserRefreshFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser principal) {
            Optional<User> current = users.findById(principal.id());
            boolean valid = current.isPresent() && current.get().isEnabled()
                    && current.get().getSecurityStamp().equals(principal.securityStamp());
            if (!valid) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}

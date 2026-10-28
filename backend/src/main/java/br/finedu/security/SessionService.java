package br.finedu.security;

import br.finedu.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Estabelece a sessão autenticada, trocando o id da sessão para evitar fixação de sessão. */
@Component
public class SessionService {

    private final SecurityContextRepository contextRepository;

    public SessionService(SecurityContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    /** Usado após cadastro ou troca de senha, quando o usuário já foi verificado. */
    public void establish(User user, HttpServletRequest request, HttpServletResponse response) {
        AuthUser principal = AppUserDetailsService.toAuthUser(user);
        principal.eraseCredentials();
        establish(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()),
                request, response);
    }
}

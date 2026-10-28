package br.finedu.security;

import br.finedu.user.User;
import br.finedu.user.UserRepository;
import br.finedu.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User u = users.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
        return toAuthUser(u);
    }

    public static AuthUser toAuthUser(User u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.isEnabled(),
                u.getSecurityStamp(), u.getPasswordHash());
    }
}

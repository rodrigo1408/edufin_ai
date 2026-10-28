package br.finedu.security;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal autenticado mantido na sessão. Contém apenas o necessário para autorização; o hash
 * da senha é apagado após a autenticação.
 */
public final class AuthUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final boolean enabled;
    private final String securityStamp;
    private String passwordHash;

    public AuthUser(Long id, String email, String displayName, Role role, boolean enabled, String securityStamp,
                    String passwordHash) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.enabled = enabled;
        this.securityStamp = securityStamp;
        this.passwordHash = passwordHash;
    }

    public Long id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Role role() {
        return role;
    }

    public String securityStamp() {
        return securityStamp;
    }

    public boolean has(Permission permission) {
        return role.has(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        role.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.name())));
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    @Override
    public String toString() {
        return "AuthUser[id=" + id + ", role=" + role + "]";
    }
}

package br.finedu.user;

import br.finedu.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "security_stamp", nullable = false, length = 36)
    private String securityStamp;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected User() {
    }

    public User(String email, String passwordHash, String displayName, Role role, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.termsAcceptedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
        this.securityStamp = UUID.randomUUID().toString();
    }

    /** Invalida sessões existentes (o filtro de sessão compara o carimbo a cada requisição). */
    public void rotateSecurityStamp(Instant now) {
        this.securityStamp = UUID.randomUUID().toString();
        this.updatedAt = now;
    }

    public void changePassword(String newHash, Instant now) {
        this.passwordHash = newHash;
        rotateSecurityStamp(now);
    }

    public void changeRole(Role newRole, Instant now) {
        this.role = newRole;
        rotateSecurityStamp(now);
    }

    public void setEnabled(boolean enabled, Instant now) {
        this.enabled = enabled;
        rotateSecurityStamp(now);
    }

    public void rename(String displayName, Instant now) {
        this.displayName = displayName;
        this.updatedAt = now;
    }

    public void changeEmail(String email, Instant now) {
        this.email = email;
        this.updatedAt = now;
    }

    public void markLogin(Instant now) {
        this.lastLoginAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSecurityStamp() {
        return securityStamp;
    }

    public Instant getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

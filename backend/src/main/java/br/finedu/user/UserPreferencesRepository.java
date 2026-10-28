package br.finedu.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /** Preferências sempre existem para usuários criados pelo cadastro; o fallback cobre dados antigos. */
    default UserPreferences getOrDefault(Long userId) {
        return findById(userId).orElseGet(() -> new UserPreferences(userId));
    }
}

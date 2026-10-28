package br.finedu.admin;

import br.finedu.security.Role;
import br.finedu.user.UserDtos;
import br.finedu.user.UserRepository;
import br.finedu.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador a partir de variáveis de ambiente (ADMIN_EMAIL / ADMIN_PASSWORD),
 * apenas se ainda não existir. Nenhuma credencial fica no código ou nas migrações.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final UserService userService;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminBootstrap(UserRepository users, UserService userService,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password,
                          @Value("${app.bootstrap-admin.display-name:Administrador}") String displayName) {
        this.users = users;
        this.userService = userService;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (users.existsByEmail(UserService.normalizeEmail(email))) {
            return;
        }
        if (!password.matches(UserDtos.PASSWORD_REGEX)) {
            log.warn("ADMIN_PASSWORD não atende à política de senha; administrador inicial não criado.");
            return;
        }
        userService.createUser(email, password, displayName, Role.ADMIN);
        log.info("Administrador inicial criado a partir das variáveis de ambiente.");
    }
}

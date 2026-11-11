
package LedgerLine.config;

import LedgerLine.entity.AppUser;
import LedgerLine.entity.Role;
import LedgerLine.repository.AppUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.bootstrap-admin.enabled",
        havingValue = "true"
)
public class FirstAdminBootstrap implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${BOOTSTRAP_ADMIN_USERNAME:}")
    private String username;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:}")
    private String email;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String password;

    public FirstAdminBootstrap(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap admin environment variables are required"
            );
        }

        if (password.length() < 12) {
            throw new IllegalStateException(
                    "Bootstrap admin password must contain at least 12 characters"
            );
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();

        var existingUser = userRepository.findByUsername(normalizedUsername);

        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() == Role.ADMIN) {
                System.out.println("Bootstrap admin already exists.");
                return;
            }

            throw new IllegalStateException(
                    "Username already belongs to a non-admin user."
            );
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "Email already belongs to another user."
            );
        }

        AppUser admin = new AppUser();

        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);

        System.out.println("First administrator created successfully.");
    }
}

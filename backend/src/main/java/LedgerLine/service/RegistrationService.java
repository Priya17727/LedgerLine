
package LedgerLine.service;

import LedgerLine.dto.RegistrationRequest;
import LedgerLine.entity.AppUser;
import LedgerLine.entity.Role;
import LedgerLine.repository.AppUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(RegistrationRequest request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        AppUser user = new AppUser();

        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(
            passwordEncoder.encode(request.getPassword())
        );

        // Public registration always creates an account holder.
        user.setRole(Role.ACCOUNT_HOLDER);

        userRepository.save(user);
    }
}

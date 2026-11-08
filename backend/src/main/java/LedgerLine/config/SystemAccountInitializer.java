
package LedgerLine.config;

import LedgerLine.entity.Account;
import LedgerLine.entity.AppUser;
import LedgerLine.entity.Role;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.AppUserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SystemAccountInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemAccountInitializer(
            AccountRepository accountRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {

        this.accountRepository = accountRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        AppUser systemUser = appUserRepository
                .findByUsername("ledgerline_system")
                .orElseGet(() -> {
                    AppUser user = new AppUser();

                    user.setUsername("ledgerline_system");
                    user.setEmail("system@ledgerline.internal");
                    user.setPasswordHash(
                            passwordEncoder.encode(
                                    UUID.randomUUID().toString()));
                    user.setRole(Role.ACCOUNT_HOLDER);

                    return appUserRepository.save(user);
                });

        if (!accountRepository.existsByAccountNumber(
                "SYSTEM_CLEARING")) {

            Account clearingAccount = new Account();

            clearingAccount.setAccountNumber("SYSTEM_CLEARING");
            clearingAccount.setUser(systemUser);
            clearingAccount.setBalance(BigDecimal.ZERO);

            accountRepository.save(clearingAccount);
        }
    }
}

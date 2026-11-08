
package LedgerLine.service;

import LedgerLine.entity.Account;
import LedgerLine.entity.AppUser;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AppUserRepository userRepository;

    public AccountService(AccountRepository accountRepository,
                          AppUserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Account createAccount(Long userId) {

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Account account = new Account();

        account.setAccountNumber(
                "LL" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase()
        );

        account.setUser(user);

        return accountRepository.save(account);
    }
}

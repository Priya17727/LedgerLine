
package LedgerLine.service;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import LedgerLine.entity.Account;
import LedgerLine.entity.AppUser;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.AppUserRepository;

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
    public List<Account> getMyAccounts(String username) {

    AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found"));

    return accountRepository.findByUserId(user.getId());
}

public Account getAccountBalance(
        String username, String accountNumber) {

    return accountRepository
            .findByAccountNumberAndUserUsername(
                    accountNumber, username)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Account not found or access denied"));
}
}

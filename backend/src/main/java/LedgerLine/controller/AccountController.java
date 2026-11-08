
package LedgerLine.controller;

import java.util.Map;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import LedgerLine.entity.Account;
import LedgerLine.entity.AppUser;
import LedgerLine.repository.AppUserRepository;
import LedgerLine.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AppUserRepository userRepository;

    public AccountController(
            AccountService accountService,
            AppUserRepository userRepository) {

        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAccount(
            Authentication authentication) {

        AppUser user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Account account =
                accountService.createAccount(user.getId());

        Map<String, Object> response = Map.of(
                "message", "Account created successfully",
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance(),
                "createdAt", account.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // View all accounts belonging to the logged-in user
    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyAccounts(
            Authentication authentication) {

        List<Map<String, Object>> accounts = accountService
                .getMyAccounts(authentication.getName())
                .stream()
                .map(account -> Map.<String, Object>of(
                        "accountNumber", account.getAccountNumber(),
                        "balance", account.getBalance(),
                        "createdAt", account.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(accounts);
    }

    // View balance of a specific account
    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(
            Authentication authentication,
            @PathVariable String accountNumber) {

        Account account = accountService.getAccountBalance(
                authentication.getName(), accountNumber);

        return ResponseEntity.ok(Map.of(
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance()));
    }
}

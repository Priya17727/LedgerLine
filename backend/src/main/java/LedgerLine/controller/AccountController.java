
package LedgerLine.controller;

import LedgerLine.entity.Account;
import LedgerLine.entity.AppUser;
import LedgerLine.repository.AppUserRepository;
import LedgerLine.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}

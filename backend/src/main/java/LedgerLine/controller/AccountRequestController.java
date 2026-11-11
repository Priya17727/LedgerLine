
package LedgerLine.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import LedgerLine.entity.Account;
import LedgerLine.entity.AccountRequest;
import LedgerLine.service.AccountRequestService;

@RestController
@RequestMapping("/api/account-requests")
public class AccountRequestController {

    private final AccountRequestService requestService;

    public AccountRequestController(
            AccountRequestService requestService) {
        this.requestService = requestService;
    }

    // User submits an account request
    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRequest(
            Authentication authentication) {

        AccountRequest request =
                requestService.createRequest(authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Account request submitted",
                        "requestId", request.getId(),
                        "status", request.getStatus()
                )
        );
    }

    // User views their own requests
    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyRequests(
            Authentication authentication) {

        List<Map<String, Object>> requests =
                requestService.getMyRequests(authentication.getName())
                        .stream()
                        .map(request -> Map.<String, Object>of(
                                "requestId", request.getId(),
                                "status", request.getStatus(),
                                "requestedAt", request.getRequestedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(requests);
    }

    // Admin views pending requests
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingRequests() {

        List<Map<String, Object>> requests =
                requestService.getPendingRequests()
                        .stream()
                        .map(request -> Map.<String, Object>of(
                                "requestId", request.getId(),
                                "username", request.getUser().getUsername(),
                                "email", request.getUser().getEmail(),
                                "status", request.getStatus(),
                                "requestedAt", request.getRequestedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(requests);
    }

    // Admin approves a request
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        Account account = requestService.approveRequest(
                requestId, authentication.getName());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Account request approved",
                        "accountNumber", account.getAccountNumber(),
                        "balance", account.getBalance()
                )
        );
    }

    // Admin rejects a request
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        AccountRequest request = requestService.rejectRequest(
                requestId,
                authentication.getName(),
                body.get("reason")
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Account request rejected",
                        "requestId", request.getId(),
                        "status", request.getStatus()
                )
        );
    }
}

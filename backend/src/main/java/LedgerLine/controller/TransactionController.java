package LedgerLine.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import LedgerLine.dto.AuditTransactionResponse;
import LedgerLine.dto.TransactionHistoryResponse;
import LedgerLine.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = transactionService.deposit(
                authentication.getName(),
                request.get("accountNumber"),
                new BigDecimal(request.get("amount")),
                idempotencyKey
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = transactionService.withdraw(
                authentication.getName(),
                request.get("accountNumber"),
                new BigDecimal(request.get("amount")),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = transactionService.transfer(
                authentication.getName(),
                request.get("senderAccountNumber"),
                request.get("receiverAccountNumber"),
                new BigDecimal(request.get("amount")),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

   @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
@GetMapping("/history/{accountNumber}")
public ResponseEntity<List<TransactionHistoryResponse>> getHistory(
        Authentication authentication,
        @PathVariable String accountNumber) {

    List<TransactionHistoryResponse> history =
            transactionService.getHistory(
                    authentication.getName(),
                    accountNumber
            );

    return ResponseEntity.ok(history);
}

    @GetMapping("/audit")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<List<AuditTransactionResponse>> getAuditReport() {

        List<AuditTransactionResponse> report =
                transactionService.getAllTransactionsForAudit();

        return ResponseEntity.ok(report);
    }
}
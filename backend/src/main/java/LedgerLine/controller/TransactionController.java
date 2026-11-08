
package LedgerLine.controller;

import LedgerLine.entity.MoneyTransaction;
import LedgerLine.service.TransactionService;
import LedgerLine.dto.TransactionHistoryResponse;
import LedgerLine.dto.AuditTransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Map;

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
            @RequestBody Map<String, String> request) {

        MoneyTransaction transaction = transactionService.deposit(
                authentication.getName(),
                request.get("accountNumber"),
                new BigDecimal(request.get("amount"))
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Deposit successful",
                        "transactionId", transaction.getId(),
                        "type", transaction.getType(),
                        "amount", transaction.getAmount(),
                        "status", transaction.getStatus()
                )
        );
    }
 @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping("/withdraw")
   
    public ResponseEntity<Map<String, Object>> withdraw(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        MoneyTransaction transaction = transactionService.withdraw(
                authentication.getName(),
                request.get("accountNumber"),
                new BigDecimal(request.get("amount"))
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Withdrawal successful",
                        "transactionId", transaction.getId(),
                        "type", transaction.getType(),
                        "amount", transaction.getAmount(),
                        "status", transaction.getStatus()
                )
        );
    }
     @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    @PostMapping("/transfer")
   
public ResponseEntity<Map<String, Object>> transfer(
        Authentication authentication,
        @RequestBody Map<String, String> request) {

    List<MoneyTransaction> transactions =
            transactionService.transfer(
                    authentication.getName(),
                    request.get("senderAccountNumber"),
                    request.get("receiverAccountNumber"),
                    new BigDecimal(request.get("amount"))
            );

    return ResponseEntity.ok(
            Map.of(
                    "message", "Transfer successful",
                    "senderTransactionId", transactions.get(0).getId(),
                    "receiverTransactionId", transactions.get(1).getId(),
                    "amount", transactions.get(0).getAmount(),
                    "status", "SUCCESS"
            )
    );
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
            )
            .stream()
            .map(transaction -> new TransactionHistoryResponse(
                    transaction.getId(),
                    transaction.getType().name(),
                    transaction.getAmount(),
                    transaction.getStatus(),
                    transaction.getCreatedAt()
            ))
            .collect(Collectors.toList());

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

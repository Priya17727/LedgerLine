
package LedgerLine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuditTransactionResponse {

    private Long transactionId;
    private String accountNumber;
    private String username;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public AuditTransactionResponse(
            Long transactionId,
            String accountNumber,
            String username,
            String type,
            BigDecimal amount,
            String status,
            LocalDateTime createdAt) {

        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

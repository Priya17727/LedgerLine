package LedgerLine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryResponse {

    private Long transactionId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public TransactionHistoryResponse(
            Long transactionId,
            String type,
            BigDecimal amount,
            String status,
            LocalDateTime createdAt) {

        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getTransactionId() {
        return transactionId;
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
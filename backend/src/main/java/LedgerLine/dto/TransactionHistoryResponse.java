
package LedgerLine.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryResponse {

    private Long transactionId;
    private String type;
    private BigDecimal amount;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
    private String status;
    private LocalDateTime createdAt;

    public TransactionHistoryResponse(
            Long transactionId,
            String type,
            BigDecimal amount,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance,
            String status,
            LocalDateTime createdAt) {

        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.debit = debit;
        this.credit = credit;
        this.runningBalance = runningBalance;
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

    public BigDecimal getDebit() {
        return debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package LedgerLine.service;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import LedgerLine.entity.AuditLog;
import LedgerLine.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    public void log(
            String username,
            String action,
            String accountNumber,
            Long transactionId,
            BigDecimal amount) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setAccountNumber(accountNumber);
        auditLog.setTransactionId(transactionId);
        auditLog.setAmount(amount);

        auditLogRepository.save(auditLog);
    }
    public List<AuditLog> getAllAuditLogs() {
    return auditLogRepository.findAllByOrderByCreatedAtDesc();
}
}
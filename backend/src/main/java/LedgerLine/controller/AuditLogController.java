package LedgerLine.controller;
import java.util.List;
import LedgerLine.entity.AuditLog;
import LedgerLine.service.AuditLogService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('AUDITOR')")
    public List<AuditLog> getAllAuditLogs() {
        return auditLogService.getAllAuditLogs();
    }
}
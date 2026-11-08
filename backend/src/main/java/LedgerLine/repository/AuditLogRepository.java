package LedgerLine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import LedgerLine.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByUsernameOrderByCreatedAtDesc(String username);
}
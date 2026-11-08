
package LedgerLine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import LedgerLine.entity.LedgerEntry;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    List<LedgerEntry> findByAccountIdOrderByCreatedAtAscIdAsc(Long accountId);
}

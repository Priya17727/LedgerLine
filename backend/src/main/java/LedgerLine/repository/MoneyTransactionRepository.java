
package LedgerLine.repository;

import LedgerLine.entity.MoneyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoneyTransactionRepository
        extends JpaRepository<MoneyTransaction, Long> {

    List<MoneyTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}

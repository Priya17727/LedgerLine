
package LedgerLine.repository;

import LedgerLine.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberAndUserUsername(
            String accountNumber, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByAccountNumberAndUserUsername(
            String accountNumber, String username);

    List<Account> findByUserId(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Account> findWithLockByAccountNumber(String accountNumber);
}

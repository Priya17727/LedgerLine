
package LedgerLine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import LedgerLine.entity.AccountRequest;
import LedgerLine.entity.RequestStatus;
import jakarta.persistence.LockModeType;

public interface AccountRequestRepository
        extends JpaRepository<AccountRequest, Long> {

    List<AccountRequest> findByUserUsernameOrderByRequestedAtDesc(
            String username);

    List<AccountRequest> findByStatusOrderByRequestedAtAsc(
            RequestStatus status);

    @Query("""
            SELECT r FROM AccountRequest r
            JOIN FETCH r.user
            WHERE r.status = :status
            ORDER BY r.requestedAt ASC
            """)
    List<AccountRequest> findPendingRequestsWithUser(
            @Param("status") RequestStatus status);

    Optional<AccountRequest> findByIdAndUserUsername(
            Long id, String username);

    boolean existsByUserIdAndStatus(
            Long userId, RequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AccountRequest r WHERE r.id = :requestId")
    Optional<AccountRequest> findByIdForUpdate(
            @Param("requestId") Long requestId);
}


package LedgerLine.repository;

import LedgerLine.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO idempotency_records
        (username, idempotency_key, operation, request_hash, created_at)
        VALUES (:username, :key, :operation, :requestHash, CURRENT_TIMESTAMP)
        ON CONFLICT (username, idempotency_key) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("username") String username,
            @Param("key") String key,
            @Param("operation") String operation,
            @Param("requestHash") String requestHash
    );

    @Query("""
        SELECT r FROM IdempotencyRecord r
        WHERE r.username = :username
        AND r.idempotencyKey = :key
        """)
    Optional<IdempotencyRecord> findRecord(
            @Param("username") String username,
            @Param("key") String key
    );
}

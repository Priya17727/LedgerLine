
package LedgerLine.service;

import LedgerLine.entity.Account;
import LedgerLine.entity.IdempotencyRecord;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.MoneyTransactionRepository;
import LedgerLine.repository.LedgerEntryRepository;
import LedgerLine.repository.IdempotencyRecordRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.DeserializationFeature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MoneyTransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectReader objectReader;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void withdrawalShouldBeRejectedWhenBalanceIsInsufficient() {

        String username = "test-user";
        String accountNumber = "TEST-ACCOUNT-001";
        String idempotencyKey = "withdraw-test-001";

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setBalance(new BigDecimal("100.00"));

        IdempotencyRecord record = new IdempotencyRecord();

        when(idempotencyRepository.insertIfAbsent(
                eq(username),
                eq(idempotencyKey),
                eq("WITHDRAWAL"),
                anyString()
        )).thenAnswer(invocation -> {
            String requestHash = invocation.getArgument(3);
            record.setRequestHash(requestHash);
            return 1;
        });

        when(idempotencyRepository.findRecord(
                username,
                idempotencyKey
        )).thenReturn(Optional.of(record));

        when(accountRepository
                .findWithLockByAccountNumberAndUserUsername(
                        accountNumber,
                        username
                )).thenReturn(Optional.of(account));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.withdraw(
                        username,
                        accountNumber,
                        new BigDecimal("150.00"),
                        idempotencyKey
                )
        );

        assertEquals("Insufficient balance", exception.getMessage());

        assertEquals(
                new BigDecimal("100.00"),
                account.getBalance(),
                "Account balance must remain unchanged"
        );

        verify(accountRepository, never()).save(any(Account.class));

        verify(accountRepository, never())
                .findWithLockByAccountNumber("SYSTEM_CLEARING");

        verifyNoInteractions(
                transactionRepository,
                ledgerEntryRepository,
                auditLogService
        );

        verify(idempotencyRepository, never()).save(any());
    }

    @Test
    void duplicateWithdrawalShouldReturnPreviouslyStoredResponse()
            throws Exception {

        String username = "test-user";
        String accountNumber = "TEST-ACCOUNT-001";
        String idempotencyKey = "duplicate-withdraw-001";

        BigDecimal amount = new BigDecimal("20.00");

        IdempotencyRecord record = new IdempotencyRecord();
        record.setResponseJson("{\"message\":\"Withdrawal successful\"}");

        Map<String, Object> cachedResponse = Map.of(
                "message", "Withdrawal successful",
                "transactionId", 123L,
                "type", "WITHDRAWAL",
                "amount", amount,
                "status", "SUCCESS"
        );

        when(idempotencyRepository.insertIfAbsent(
                eq(username),
                eq(idempotencyKey),
                eq("WITHDRAWAL"),
                anyString()
        )).thenAnswer(invocation -> {
            record.setRequestHash(invocation.getArgument(3));
            return 0;
        });

        when(idempotencyRepository.findRecord(
                username,
                idempotencyKey
        )).thenReturn(Optional.of(record));

        when(objectMapper.readerFor(Map.class))
                .thenReturn(objectReader);

        when(objectReader.with(
                DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
        )).thenReturn(objectReader);

        when(objectReader.readValue(record.getResponseJson()))
                .thenReturn(cachedResponse);

        Map<String, Object> result = transactionService.withdraw(
                username,
                accountNumber,
                amount,
                idempotencyKey
        );

        assertEquals(cachedResponse, result);

        verify(idempotencyRepository).insertIfAbsent(
                eq(username),
                eq(idempotencyKey),
                eq("WITHDRAWAL"),
                anyString()
        );

        verify(idempotencyRepository).findRecord(
                username,
                idempotencyKey
        );

        verify(idempotencyRepository, never()).save(any());

        verifyNoInteractions(
                accountRepository,
                transactionRepository,
                ledgerEntryRepository,
                auditLogService
        );
    }
    @Test
    void reusedIdempotencyKeyWithDifferentRequestShouldBeRejected() {

        String username = "test-user";
        String accountNumber = "TEST-ACCOUNT-001";
        String idempotencyKey = "reuse-key-001";

        IdempotencyRecord record = new IdempotencyRecord();

        record.setRequestHash("different-request-hash");

        when(idempotencyRepository.insertIfAbsent(
                eq(username),
                eq(idempotencyKey),
                eq("WITHDRAWAL"),
                anyString()
        )).thenReturn(0);

        when(idempotencyRepository.findRecord(
                username,
                idempotencyKey
        )).thenReturn(Optional.of(record));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.withdraw(
                        username,
                        accountNumber,
                        new BigDecimal("50.00"),
                        idempotencyKey
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                accountRepository,
                transactionRepository,
                ledgerEntryRepository,
                auditLogService
        );
    }
}

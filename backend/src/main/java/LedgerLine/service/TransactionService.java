
package LedgerLine.service;

import LedgerLine.entity.Account;
import LedgerLine.entity.MoneyTransaction;
import LedgerLine.entity.TransactionType;
import LedgerLine.entity.LedgerEntry;
import LedgerLine.repository.LedgerEntryRepository;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.MoneyTransactionRepository;
import LedgerLine.dto.AuditTransactionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import LedgerLine.entity.IdempotencyRecord;
import LedgerLine.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
private final ObjectMapper objectMapper;
    public TransactionService(
        AccountRepository accountRepository,
        MoneyTransactionRepository transactionRepository,
        LedgerEntryRepository ledgerEntryRepository,
        IdempotencyRecordRepository idempotencyRepository,
        ObjectMapper objectMapper) {

    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.idempotencyRepository = idempotencyRepository;
    this.objectMapper = objectMapper;
}

  
@Transactional
@SuppressWarnings("unchecked")
public Map<String, Object> deposit(
        String username,
        String accountNumber,
        BigDecimal amount,
        String idempotencyKey) {

    return (Map<String, Object>) executeIdempotently(
            username,
            idempotencyKey,
            "DEPOSIT",
            accountNumber + "|" + amount.toPlainString(),
            Map.class,
            () -> {
                MoneyTransaction transaction =
                        depositInternal(username, accountNumber, amount);

                return Map.of(
                        "message", "Deposit successful",
                        "transactionId", transaction.getId(),
                        "type", transaction.getType().name(),
                        "amount", transaction.getAmount(),
                        "status", transaction.getStatus()
                );
            }
    );
}

private MoneyTransaction depositInternal(
        String username,
        String accountNumber,
        BigDecimal amount) {

    validateAmount(amount);

    Account account = getAccount(username, accountNumber);

    Account clearingAccount = accountRepository
            .findWithLockByAccountNumber("SYSTEM_CLEARING")
            .orElseThrow(() -> new IllegalStateException(
                    "System clearing account not found"));

    account.setBalance(account.getBalance().add(amount));

    MoneyTransaction transaction = new MoneyTransaction();
    transaction.setAccount(account);
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setAmount(amount);
    transaction.setStatus("SUCCESS");

    accountRepository.save(account);

    MoneyTransaction savedTransaction =
            transactionRepository.save(transaction);

    LedgerEntry customerEntry = new LedgerEntry();
    customerEntry.setTransaction(savedTransaction);
    customerEntry.setAccount(account);
    customerEntry.setDebit(BigDecimal.ZERO);
    customerEntry.setCredit(amount);

    LedgerEntry clearingEntry = new LedgerEntry();
    clearingEntry.setTransaction(savedTransaction);
    clearingEntry.setAccount(clearingAccount);
    clearingEntry.setDebit(amount);
    clearingEntry.setCredit(BigDecimal.ZERO);

    ledgerEntryRepository.saveAll(
            List.of(customerEntry, clearingEntry));

    return savedTransaction;
}

  
@Transactional
@SuppressWarnings("unchecked")
public Map<String, Object> withdraw(
        String username,
        String accountNumber,
        BigDecimal amount,
        String idempotencyKey) {

    return (Map<String, Object>) executeIdempotently(
            username,
            idempotencyKey,
            "WITHDRAWAL",
            accountNumber + "|" + amount.toPlainString(),
            Map.class,
            () -> {
                MoneyTransaction transaction =
                        withdrawInternal(username, accountNumber, amount);

                return Map.of(
                        "message", "Withdrawal successful",
                        "transactionId", transaction.getId(),
                        "type", transaction.getType().name(),
                        "amount", transaction.getAmount(),
                        "status", transaction.getStatus()
                );
            }
    );
}

private MoneyTransaction withdrawInternal(
        String username,
        String accountNumber,
        BigDecimal amount) {

    validateAmount(amount);

    Account account = getAccount(username, accountNumber);

    if (account.getBalance().compareTo(amount) < 0) {
        throw new IllegalArgumentException("Insufficient balance");
    }

    Account clearingAccount = accountRepository
            .findWithLockByAccountNumber("SYSTEM_CLEARING")
            .orElseThrow(() -> new IllegalStateException(
                    "System clearing account not found"));

    account.setBalance(account.getBalance().subtract(amount));

    MoneyTransaction transaction = new MoneyTransaction();
    transaction.setAccount(account);
    transaction.setType(TransactionType.WITHDRAWAL);
    transaction.setAmount(amount);
    transaction.setStatus("SUCCESS");

    accountRepository.save(account);

    MoneyTransaction savedTransaction =
            transactionRepository.save(transaction);

    LedgerEntry debitEntry = new LedgerEntry();
    debitEntry.setTransaction(savedTransaction);
    debitEntry.setAccount(account);
    debitEntry.setDebit(amount);
    debitEntry.setCredit(BigDecimal.ZERO);

    LedgerEntry creditEntry = new LedgerEntry();
    creditEntry.setTransaction(savedTransaction);
    creditEntry.setAccount(clearingAccount);
    creditEntry.setDebit(BigDecimal.ZERO);
    creditEntry.setCredit(amount);

    ledgerEntryRepository.saveAll(
            List.of(debitEntry, creditEntry));

    return savedTransaction;
}


@Transactional
@SuppressWarnings("unchecked")
public Map<String, Object> transfer(
        String username,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        String idempotencyKey) {

    return (Map<String, Object>) executeIdempotently(
            username,
            idempotencyKey,
            "TRANSFER",
            senderAccountNumber + "|" +
                    receiverAccountNumber + "|" +
                    amount.toPlainString(),
            Map.class,
            () -> {
                List<MoneyTransaction> transactions =
                        transferInternal(
                                username,
                                senderAccountNumber,
                                receiverAccountNumber,
                                amount
                        );

                return Map.of(
                        "message", "Transfer successful",
                        "senderTransactionId", transactions.get(0).getId(),
                        "receiverTransactionId", transactions.get(1).getId(),
                        "amount", transactions.get(0).getAmount(),
                        "status", "SUCCESS"
                );
            }
    );
}

private List<MoneyTransaction> transferInternal(
        String username,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount) {

    validateAmount(amount);

    if (senderAccountNumber.equals(receiverAccountNumber)) {
        throw new IllegalArgumentException(
                "Cannot transfer to the same account");
    }

    String firstNumber = senderAccountNumber.compareTo(
            receiverAccountNumber) < 0
            ? senderAccountNumber : receiverAccountNumber;

    String secondNumber = firstNumber.equals(senderAccountNumber)
            ? receiverAccountNumber : senderAccountNumber;

    Account first = accountRepository
            .findWithLockByAccountNumber(firstNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Account not found"));

    Account second = accountRepository
            .findWithLockByAccountNumber(secondNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Account not found"));

    Account sender = senderAccountNumber.equals(firstNumber)
            ? first : second;

    Account receiver = receiverAccountNumber.equals(firstNumber)
            ? first : second;

    if (!sender.getUser().getUsername().equals(username)) {
        throw new IllegalArgumentException(
                "Account not found or access denied");
    }

    if (sender.getBalance().compareTo(amount) < 0) {
        throw new IllegalArgumentException("Insufficient balance");
    }

    sender.setBalance(sender.getBalance().subtract(amount));
    receiver.setBalance(receiver.getBalance().add(amount));

    MoneyTransaction outgoing = new MoneyTransaction();
    outgoing.setAccount(sender);
    outgoing.setType(TransactionType.TRANSFER_OUT);
    outgoing.setAmount(amount);
    outgoing.setStatus("SUCCESS");

    MoneyTransaction incoming = new MoneyTransaction();
    incoming.setAccount(receiver);
    incoming.setType(TransactionType.TRANSFER_IN);
    incoming.setAmount(amount);
    incoming.setStatus("SUCCESS");

    accountRepository.save(sender);
    accountRepository.save(receiver);

    MoneyTransaction savedOutgoing =
            transactionRepository.save(outgoing);

    MoneyTransaction savedIncoming =
            transactionRepository.save(incoming);

    LedgerEntry debitEntry = new LedgerEntry();
    debitEntry.setTransaction(savedOutgoing);
    debitEntry.setAccount(sender);
    debitEntry.setDebit(amount);
    debitEntry.setCredit(BigDecimal.ZERO);

    LedgerEntry creditEntry = new LedgerEntry();
    creditEntry.setTransaction(savedIncoming);
    creditEntry.setAccount(receiver);
    creditEntry.setDebit(BigDecimal.ZERO);
    creditEntry.setCredit(amount);

    ledgerEntryRepository.saveAll(
            List.of(debitEntry, creditEntry));

    return List.of(savedOutgoing, savedIncoming);
}

    @Transactional(readOnly = true)
    public List<MoneyTransaction> getHistory(
            String username, String accountNumber) {

        Account account = accountRepository
                .findByAccountNumberAndUserUsername(accountNumber, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found or access denied"));

        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(account.getId());
    }
@Transactional(readOnly = true)
public List<AuditTransactionResponse> getAllTransactionsForAudit() {

    return transactionRepository.findAll()
            .stream()
            .map(transaction -> new AuditTransactionResponse(
                    transaction.getId(),
                    transaction.getAccount().getAccountNumber(),
                    transaction.getAccount().getUser().getUsername(),
                    transaction.getType().name(),
                    transaction.getAmount(),
                    transaction.getStatus(),
                    transaction.getCreatedAt()
            ))
            .toList();
}
    private Account getAccount(String username, String accountNumber) {

        return accountRepository
                .findWithLockByAccountNumberAndUserUsername(
                        accountNumber, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found or access denied"));
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {

            throw new IllegalArgumentException(
                    "Amount must be positive with at most two decimal places");
        }
    }
    private <T> T executeIdempotently(
        String username,
        String idempotencyKey,
        String operation,
        String requestData,
        Class<T> responseType,
        Supplier<T> action) {

    if (idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 100) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A valid Idempotency-Key header is required");
    }

    try {
        String requestHash = sha256(operation + "|" + requestData);

        int inserted = idempotencyRepository.insertIfAbsent(
                username,
                idempotencyKey,
                operation,
                requestHash
        );

        IdempotencyRecord record = idempotencyRepository
                .findRecord(username, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not retrieve idempotency record"));

        if (!record.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was already used for a different request");
        }

        if (inserted == 0 && record.getResponseJson() != null) {
            return objectMapper.readerFor(responseType)
        .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .readValue(record.getResponseJson());
        }

        if (inserted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request with this key is already being processed");
        }

        T response = action.get();

        record.setResponseJson(
                objectMapper.writeValueAsString(response));

        idempotencyRepository.save(record);

        return response;

    } catch (ResponseStatusException exception) {
    throw exception;
} catch (RuntimeException exception) {
    throw exception;
} catch (Exception exception) {
    throw new IllegalStateException(
            "Idempotency processing failed", exception);
}
}

private String sha256(String value) throws Exception {

    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    byte[] hash = digest.digest(
            value.getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();

    for (byte b : hash) {
        result.append(String.format("%02x", b));
    }

    return result.toString();
}
}

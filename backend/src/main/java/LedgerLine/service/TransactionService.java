
package LedgerLine.service;

import LedgerLine.entity.Account;
import LedgerLine.entity.MoneyTransaction;
import LedgerLine.entity.TransactionType;
import LedgerLine.entity.LedgerEntry;
import LedgerLine.repository.LedgerEntryRepository;
import LedgerLine.repository.AccountRepository;
import LedgerLine.repository.MoneyTransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionService(
        AccountRepository accountRepository,
        MoneyTransactionRepository transactionRepository,
        LedgerEntryRepository ledgerEntryRepository) {

    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
}

   @Transactional
public MoneyTransaction deposit(
        String username, String accountNumber, BigDecimal amount) {

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

    // Customer account: CREDIT
    LedgerEntry customerEntry = new LedgerEntry();
    customerEntry.setTransaction(savedTransaction);
    customerEntry.setAccount(account);
    customerEntry.setDebit(BigDecimal.ZERO);
    customerEntry.setCredit(amount);

    // Clearing account: DEBIT
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
public MoneyTransaction withdraw(
        String username, String accountNumber, BigDecimal amount) {

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

    // Customer account: DEBIT
    LedgerEntry debitEntry = new LedgerEntry();
    debitEntry.setTransaction(savedTransaction);
    debitEntry.setAccount(account);
    debitEntry.setDebit(amount);
    debitEntry.setCredit(BigDecimal.ZERO);

    // Clearing account: CREDIT
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
public List<MoneyTransaction> transfer(
        String username,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount) {

    validateAmount(amount);

    if (senderAccountNumber.equals(receiverAccountNumber)) {
        throw new IllegalArgumentException(
                "Cannot transfer to the same account");
    }

    // Lock accounts in a consistent order to reduce deadlocks
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

// Ledger entry for sender: DEBIT
LedgerEntry debitEntry = new LedgerEntry();
debitEntry.setTransaction(savedOutgoing);
debitEntry.setAccount(sender);
debitEntry.setDebit(amount);
debitEntry.setCredit(BigDecimal.ZERO);

// Ledger entry for receiver: CREDIT
LedgerEntry creditEntry = new LedgerEntry();
creditEntry.setTransaction(savedIncoming);
creditEntry.setAccount(receiver);
creditEntry.setDebit(BigDecimal.ZERO);
creditEntry.setCredit(amount);

// Save both ledger entries
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
}

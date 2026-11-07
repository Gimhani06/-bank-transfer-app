package com.example.bankapp.service;

import com.example.bankapp.exception.*;
import com.example.bankapp.model.*;
import com.example.bankapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final TransactionAuditService auditService;

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {
        // Step 1: Validate accounts are different
        if (fromAccount.equals(toAccount)) {
            String error = "Sender and receiver accounts must be different";
            auditService.recordFailure(fromAccount, toAccount, amount, error);
            throw new InvalidTransactionException(error);
        }

        // Validate accounts exist
        if (customerRepository.countAccounts(fromAccount) == 0 || customerRepository.countAccounts(toAccount) == 0) {
            String error = "One or both accounts do not exist";
            auditService.recordFailure(fromAccount, toAccount, amount, error);
            throw new AccountNotFoundException(error);
        }

        // Step 2: Debit sender atomically
        int rowsUpdated = customerRepository.debit(fromAccount, amount);
        if (rowsUpdated == 0) {
            String error = "Insufficient balance in account " + fromAccount;
            auditService.recordFailure(fromAccount, toAccount, amount, error);
            throw new InsufficientBalanceException(error);
        }

        // Step 3: Simulate failure experiment checkbox check
        if (simulateFailure) {
            String error = "Simulated system failure after withdrawal";
            auditService.recordFailure(fromAccount, toAccount, amount, error);
            throw new RuntimeException(error);
        }

        // Step 4: Credit receiver
        customerRepository.credit(toAccount, amount);

        // Step 5: Save SUCCESS record
        BankTransaction tx = BankTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Transfer completed")
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Account does not exist");
        }

        return transactionRepository.save(BankTransaction.builder()
                .toAccount(accountNumber)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Deposit completed")
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(
                    "Account does not exist or has insufficient balance");
        }

        return transactionRepository.save(BankTransaction.builder()
                .fromAccount(accountNumber)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .description("Withdrawal completed")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
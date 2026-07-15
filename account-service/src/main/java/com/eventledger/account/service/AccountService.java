package com.eventledger.account.service;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.AccountTransaction;
import com.eventledger.account.entity.TransactionType;
import com.eventledger.account.exception.ResourceNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.AccountTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse applyTransaction(
            String accountId,
            TransactionRequest request,
            String traceId
    ) {
        Optional<AccountTransaction> existingTransaction =
                transactionRepository.findByEventId(request.getEventId());

        if (existingTransaction.isPresent()) {
            Account existingAccount = accountRepository.findById(accountId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Account not found: " + accountId
                            )
                    );

            log.info(
                    "Duplicate transaction ignored. traceId={}, eventId={}, accountId={}",
                    traceId,
                    request.getEventId(),
                    accountId
            );

            return new TransactionResponse(
                    request.getEventId(),
                    accountId,
                    "ALREADY_APPLIED",
                    existingAccount.getBalance(),
                    true
            );
        }

        Account account = accountRepository.findById(accountId)
                .orElseGet(() -> new Account(
                        accountId,
                        BigDecimal.ZERO,
                        request.getCurrency()
                ));

        if (!account.getCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new IllegalArgumentException(
                    "Transaction currency does not match account currency"
            );
        }

        BigDecimal updatedBalance;

        if (request.getType() == TransactionType.CREDIT) {
            updatedBalance = account.getBalance().add(request.getAmount());
        } else {
            updatedBalance = account.getBalance().subtract(request.getAmount());
        }

        account.setBalance(updatedBalance);
        accountRepository.save(account);

        AccountTransaction transaction = new AccountTransaction();
        transaction.setEventId(request.getEventId());
        transaction.setAccountId(accountId);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setEventTimestamp(request.getEventTimestamp());
        transaction.setTraceId(traceId);

        transactionRepository.save(transaction);

        log.info(
                "Transaction applied. traceId={}, eventId={}, accountId={}, type={}, amount={}, balance={}",
                traceId,
                request.getEventId(),
                accountId,
                request.getType(),
                request.getAmount(),
                updatedBalance
        );

        return new TransactionResponse(
                request.getEventId(),
                accountId,
                "APPLIED",
                updatedBalance,
                false
        );
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = getAccountEntity(accountId);

        return new BalanceResponse(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency()
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = getAccountEntity(accountId);

        List<AccountTransaction> transactions =
                transactionRepository
                        .findTop10ByAccountIdOrderByEventTimestampDesc(accountId);

        return new AccountResponse(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                transactions
        );
    }

    private Account getAccountEntity(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found: " + accountId
                        )
                );
    }
}
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository transactionRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                transactionRepository
        );
    }

    @Test
    void shouldApplyCreditTransaction() {
        TransactionRequest request = createRequest(
                "evt-001",
                TransactionType.CREDIT,
                "150.00"
        );

        when(transactionRepository.findByEventId("evt-001"))
                .thenReturn(Optional.empty());

        when(accountRepository.findById("acct-123"))
                .thenReturn(Optional.empty());

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                accountService.applyTransaction(
                        "acct-123",
                        request,
                        "trace-001"
                );

        assertEquals("APPLIED", response.status());
        assertEquals(new BigDecimal("150.00"), response.balance());
        assertFalse(response.duplicate());

        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(AccountTransaction.class));
    }

    @Test
    void shouldApplyDebitTransaction() {
        TransactionRequest request = createRequest(
                "evt-002",
                TransactionType.DEBIT,
                "40.00"
        );

        Account account = new Account(
                "acct-123",
                new BigDecimal("150.00"),
                "USD"
        );

        when(transactionRepository.findByEventId("evt-002"))
                .thenReturn(Optional.empty());

        when(accountRepository.findById("acct-123"))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                accountService.applyTransaction(
                        "acct-123",
                        request,
                        "trace-002"
                );

        assertEquals(new BigDecimal("110.00"), response.balance());
        assertEquals("APPLIED", response.status());
    }

    @Test
    void shouldIgnoreDuplicateTransaction() {
        TransactionRequest request = createRequest(
                "evt-001",
                TransactionType.CREDIT,
                "150.00"
        );

        AccountTransaction existingTransaction =
                new AccountTransaction();

        Account account = new Account(
                "acct-123",
                new BigDecimal("150.00"),
                "USD"
        );

        when(transactionRepository.findByEventId("evt-001"))
                .thenReturn(Optional.of(existingTransaction));

        when(accountRepository.findById("acct-123"))
                .thenReturn(Optional.of(account));

        TransactionResponse response =
                accountService.applyTransaction(
                        "acct-123",
                        request,
                        "trace-001"
                );

        assertTrue(response.duplicate());
        assertEquals("ALREADY_APPLIED", response.status());
        assertEquals(new BigDecimal("150.00"), response.balance());

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldReturnBalance() {
        Account account = new Account(
                "acct-123",
                new BigDecimal("175.00"),
                "USD"
        );

        when(accountRepository.findById("acct-123"))
                .thenReturn(Optional.of(account));

        BalanceResponse response =
                accountService.getBalance("acct-123");

        assertEquals("acct-123", response.accountId());
        assertEquals(new BigDecimal("175.00"), response.balance());
        assertEquals("USD", response.currency());
    }

    @Test
    void shouldReturnAccountDetails() {
        Account account = new Account(
                "acct-123",
                new BigDecimal("175.00"),
                "USD"
        );

        AccountTransaction transaction =
                new AccountTransaction();

        transaction.setEventId("evt-001");
        transaction.setAccountId("acct-123");
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(new BigDecimal("175.00"));
        transaction.setCurrency("USD");
        transaction.setEventTimestamp(
                Instant.parse("2026-05-15T14:02:11Z")
        );

        when(accountRepository.findById("acct-123"))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findTop10ByAccountIdOrderByEventTimestampDesc("acct-123"))
                .thenReturn(List.of(transaction));

        AccountResponse response =
                accountService.getAccount("acct-123");

        assertEquals("acct-123", response.accountId());
        assertEquals(new BigDecimal("175.00"), response.balance());
        assertEquals(1, response.recentTransactions().size());
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getBalance("missing")
        );
    }

    private TransactionRequest createRequest(
            String eventId,
            TransactionType type,
            String amount
    ) {
        TransactionRequest request = new TransactionRequest();

        request.setEventId(eventId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        request.setCurrency("USD");
        request.setEventTimestamp(
                Instant.parse("2026-05-15T14:02:11Z")
        );

        return request;
    }
}

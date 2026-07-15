package com.eventledger.account.repository;

import com.eventledger.account.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTransactionRepository
        extends JpaRepository<AccountTransaction, Long> {

    Optional<AccountTransaction> findByEventId(String eventId);

    List<AccountTransaction>
    findTop10ByAccountIdOrderByEventTimestampDesc(String accountId);
}
package com.eventledger.account.dto;

import com.eventledger.account.entity.AccountTransaction;

import java.math.BigDecimal;
import java.util.List;

public record AccountResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        List<AccountTransaction> recentTransactions
) {
}
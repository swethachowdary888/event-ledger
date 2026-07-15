package com.eventledger.account.dto;

import java.math.BigDecimal;

public record TransactionResponse(
        String eventId,
        String accountId,
        String status,
        BigDecimal balance,
        boolean duplicate
) {
}
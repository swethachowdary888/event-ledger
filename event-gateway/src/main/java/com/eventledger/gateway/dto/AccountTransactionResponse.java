package com.eventledger.gateway.dto;

import java.math.BigDecimal;

public record AccountTransactionResponse(
        String eventId,
        String accountId,
        String status,
        BigDecimal balance,
        boolean duplicate
) {
}

package com.eventledger.account.controller;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(
                    value = TRACE_HEADER,
                    required = false
            ) String traceId
    ) {
        String effectiveTraceId =
                traceId == null || traceId.isBlank()
                        ? UUID.randomUUID().toString()
                        : traceId;

        TransactionResponse response =
                accountService.applyTransaction(
                        accountId,
                        request,
                        effectiveTraceId
                );

        HttpStatus status = response.duplicate()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .header(TRACE_HEADER, effectiveTraceId)
                .body(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(
                accountService.getBalance(accountId)
        );
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(
                accountService.getAccount(accountId)
        );
    }
}
package com.eventledger.gateway.client;

import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AccountServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(AccountServiceClient.class);

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final RestTemplate restTemplate;
    private final String accountServiceBaseUrl;

    public AccountServiceClient(
            RestTemplate restTemplate,
            @Value("${account.service.base-url}")
            String accountServiceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.accountServiceBaseUrl = accountServiceBaseUrl;
    }

    @Retryable(
            retryFor = {
                    ResourceAccessException.class,
                    RestClientException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 500,
                    multiplier = 2.0
            )
    )
    public AccountTransactionResponse applyTransaction(
            String accountId,
            AccountTransactionRequest request,
            String traceId
    ) {
        String url = accountServiceBaseUrl
                + "/accounts/"
                + accountId
                + "/transactions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(TRACE_HEADER, traceId);

        HttpEntity<AccountTransactionRequest> httpEntity =
                new HttpEntity<>(request, headers);

        log.info(
                "Calling Account Service. traceId={}, accountId={}, eventId={}",
                traceId,
                accountId,
                request.eventId()
        );

        ResponseEntity<AccountTransactionResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        httpEntity,
                        AccountTransactionResponse.class
                );

        return response.getBody();
    }

    @Recover
    public AccountTransactionResponse recover(
            RestClientException exception,
            String accountId,
            AccountTransactionRequest request,
            String traceId
    ) {
        log.error(
                "Account Service failed after retries. traceId={}, accountId={}, eventId={}",
                traceId,
                accountId,
                request.eventId(),
                exception
        );

        throw new AccountServiceUnavailableException(
                "Account Service is unavailable after 3 attempts",
                exception
        );
    }
}
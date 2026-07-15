package com.eventledger.gateway.integration;

import com.eventledger.gateway.dto.EventDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class EventGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate =
            new TestRestTemplate();

    @Test
    void shouldCreateEventThroughGateway() {
        String eventId =
                "evt-integration-" + System.currentTimeMillis();

        EventDto request = new EventDto();
        request.setEventId(eventId);
        request.setAccountId("acct-integration");
        request.setType("CREDIT");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());
        request.setMetadata(
                Map.of("source", "integration-test")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Trace-Id", "trace-integration-001");

        HttpEntity<EventDto> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<EventDto> response =
                restTemplate.exchange(
                        "http://localhost:"
                                + port
                                + "/events",
                        HttpMethod.POST,
                        entity,
                        EventDto.class
                );

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                eventId,
                response.getBody().getEventId()
        );

        assertEquals(
                "trace-integration-001",
                response.getHeaders()
                        .getFirst("X-Trace-Id")
        );
    }
}
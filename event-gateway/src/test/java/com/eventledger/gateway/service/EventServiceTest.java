package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.dto.EventDto;
import com.eventledger.gateway.entity.Event;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.repository.EventRepository;
import com.eventledger.gateway.util.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AccountServiceClient accountServiceClient;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                eventMapper,
                accountServiceClient,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void shouldCreateCreditEventSuccessfully() {
        EventDto request = createEventDto(
                "evt-001",
                "CREDIT",
                "150.00",
                "2026-05-15T14:02:11Z"
        );

        Event entity = createEventEntity(
                "evt-001",
                "CREDIT",
                "150.00",
                "2026-05-15T14:02:11Z"
        );

        when(eventRepository.findById("evt-001"))
                .thenReturn(Optional.empty());

        when(eventMapper.toEntity(request))
                .thenReturn(entity);

        when(eventRepository.save(entity))
                .thenReturn(entity);

        when(eventMapper.toDto(entity))
                .thenReturn(request);

        when(accountServiceClient.applyTransaction(
                eq("acct-123"),
                any(),
                eq("trace-001")
        )).thenReturn(
                new AccountTransactionResponse(
                        "evt-001",
                        "acct-123",
                        "APPLIED",
                        new BigDecimal("150.00"),
                        false
                )
        );

        CreateEventResult result =
                eventService.createEvent(request, "trace-001");

        assertNotNull(result);
        assertFalse(result.duplicate());
        assertEquals("evt-001", result.event().getEventId());

        verify(accountServiceClient, times(1))
                .applyTransaction(
                        eq("acct-123"),
                        any(),
                        eq("trace-001")
                );

        verify(eventRepository, times(1)).save(entity);
    }

    @Test
    void shouldReturnExistingEventForDuplicateEventId() {
        EventDto existingDto = createEventDto(
                "evt-001",
                "CREDIT",
                "150.00",
                "2026-05-15T14:02:11Z"
        );

        Event existingEntity = createEventEntity(
                "evt-001",
                "CREDIT",
                "150.00",
                "2026-05-15T14:02:11Z"
        );

        when(eventRepository.findById("evt-001"))
                .thenReturn(Optional.of(existingEntity));

        when(eventMapper.toDto(existingEntity))
                .thenReturn(existingDto);

        CreateEventResult result =
                eventService.createEvent(existingDto, "trace-001");

        assertTrue(result.duplicate());
        assertEquals("evt-001", result.event().getEventId());

        verify(accountServiceClient, never())
                .applyTransaction(anyString(), any(), anyString());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownTransactionType() {
        EventDto request = createEventDto(
                "evt-002",
                "TRANSFER",
                "50.00",
                "2026-05-15T14:02:11Z"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> eventService.createEvent(
                                request,
                                "trace-002"
                        )
                );

        assertEquals(
                "type must be CREDIT or DEBIT",
                exception.getMessage()
        );

        verifyNoInteractions(accountServiceClient);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void shouldThrowExceptionWhenEventIsNotFound() {
        when(eventRepository.findById("evt-missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                EventNotFoundException.class,
                () -> eventService.getEvent("evt-missing")
        );
    }

    @Test
    void shouldReturnEventsForAccountInRepositoryOrder() {
        Event first = createEventEntity(
                "evt-early",
                "DEBIT",
                "20.00",
                "2026-05-14T10:00:00Z"
        );

        Event second = createEventEntity(
                "evt-late",
                "CREDIT",
                "100.00",
                "2026-05-15T14:00:00Z"
        );

        EventDto firstDto = createEventDto(
                "evt-early",
                "DEBIT",
                "20.00",
                "2026-05-14T10:00:00Z"
        );

        EventDto secondDto = createEventDto(
                "evt-late",
                "CREDIT",
                "100.00",
                "2026-05-15T14:00:00Z"
        );

        when(eventRepository
                .findByAccountIdOrderByEventTimestampAsc("acct-123"))
                .thenReturn(List.of(first, second));

        when(eventMapper.toDto(first)).thenReturn(firstDto);
        when(eventMapper.toDto(second)).thenReturn(secondDto);

        List<EventDto> results =
                eventService.getEventsByAccount("acct-123");

        assertEquals(2, results.size());
        assertEquals("evt-early", results.get(0).getEventId());
        assertEquals("evt-late", results.get(1).getEventId());
    }

    private EventDto createEventDto(
            String eventId,
            String type,
            String amount,
            String timestamp
    ) {
        EventDto dto = new EventDto();

        dto.setEventId(eventId);
        dto.setAccountId("acct-123");
        dto.setType(type);
        dto.setAmount(new BigDecimal(amount));
        dto.setCurrency("USD");
        dto.setEventTimestamp(Instant.parse(timestamp));

        return dto;
    }

    private Event createEventEntity(
            String eventId,
            String type,
            String amount,
            String timestamp
    ) {
        Event event = new Event();

        event.setEventId(eventId);
        event.setAccountId("acct-123");
        event.setType(type);
        event.setAmount(new BigDecimal(amount));
        event.setCurrency("USD");
        event.setEventTimestamp(Instant.parse(timestamp));

        return event;
    }
}

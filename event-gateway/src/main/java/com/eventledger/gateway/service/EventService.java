package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.EventDto;
import com.eventledger.gateway.entity.Event;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.repository.EventRepository;
import com.eventledger.gateway.util.EventMapper;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final AccountServiceClient accountServiceClient;
    private final Counter eventsCreatedCounter;
    public EventService(
            EventRepository eventRepository,
            EventMapper eventMapper,
            AccountServiceClient accountServiceClient,
            MeterRegistry meterRegistry
    ) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.accountServiceClient = accountServiceClient;

        this.eventsCreatedCounter = Counter.builder("event_gateway_events_created")
                .description("Number of events successfully created")
                .register(meterRegistry);
    }

    public CreateEventResult createEvent(
            EventDto eventDto,
            String traceId
    ) {
        validateTransactionType(eventDto.getType());

        Event existingEvent = eventRepository
                .findById(eventDto.getEventId())
                .orElse(null);

        if (existingEvent != null) {
            return new CreateEventResult(
                    eventMapper.toDto(existingEvent),
                    true
            );
        }

        AccountTransactionRequest accountRequest =
                new AccountTransactionRequest(
                        eventDto.getEventId(),
                        eventDto.getType().toUpperCase(),
                        eventDto.getAmount(),
                        eventDto.getCurrency().toUpperCase(),
                        eventDto.getEventTimestamp()
                );

        accountServiceClient.applyTransaction(
                eventDto.getAccountId(),
                accountRequest,
                traceId
        );

        Event savedEvent = eventRepository.save(
                eventMapper.toEntity(eventDto)
        );

        eventsCreatedCounter.increment();

        return new CreateEventResult(
                eventMapper.toDto(savedEvent),
                false
        );
    }

    public EventDto getEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EventNotFoundException(
                                "Event not found: " + eventId
                        )
                );

        return eventMapper.toDto(event);
    }

    public List<EventDto> getEventsByAccount(String accountId) {
        return eventRepository
                .findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    private void validateTransactionType(String type) {
        if (type == null ||
                (!type.equalsIgnoreCase("CREDIT")
                        && !type.equalsIgnoreCase("DEBIT"))) {

            throw new IllegalArgumentException(
                    "type must be CREDIT or DEBIT"
            );
        }
    }
}
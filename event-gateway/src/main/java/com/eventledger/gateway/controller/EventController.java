package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.EventDto;
import com.eventledger.gateway.service.CreateEventResult;
import com.eventledger.gateway.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private static final String TRACE_HEADER = "X-Trace-Id";

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventDto> createEvent(
            @Valid @RequestBody EventDto eventDto,
            @RequestHeader(
                    value = TRACE_HEADER,
                    required = false
            ) String traceId
    ) {
        String effectiveTraceId =
                traceId == null || traceId.isBlank()
                        ? UUID.randomUUID().toString()
                        : traceId;

        CreateEventResult result =
                eventService.createEvent(
                        eventDto,
                        effectiveTraceId
                );

        HttpStatus status = result.duplicate()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .header(TRACE_HEADER, effectiveTraceId)
                .body(result.event());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEvent(
            @PathVariable String eventId
    ) {
        return ResponseEntity.ok(
                eventService.getEvent(eventId)
        );
    }

    @GetMapping
    public ResponseEntity<List<EventDto>> getEventsByAccount(
            @RequestParam("account") String accountId
    ) {
        return ResponseEntity.ok(
                eventService.getEventsByAccount(accountId)
        );
    }
}
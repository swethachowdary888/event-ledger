package com.eventledger.gateway.util;

import com.eventledger.gateway.dto.EventDto;
import com.eventledger.gateway.entity.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventMapper {

    private final ObjectMapper objectMapper;

    public EventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Event toEntity(EventDto dto) {
        Event event = new Event();

        event.setEventId(dto.getEventId());
        event.setAccountId(dto.getAccountId());
        event.setType(dto.getType().toUpperCase());
        event.setAmount(dto.getAmount());
        event.setCurrency(dto.getCurrency().toUpperCase());
        event.setEventTimestamp(dto.getEventTimestamp());

        if (dto.getMetadata() != null) {
            try {
                event.setMetadata(
                        objectMapper.writeValueAsString(dto.getMetadata())
                );
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Invalid metadata");
            }
        }

        return event;
    }

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();

        dto.setEventId(event.getEventId());
        dto.setAccountId(event.getAccountId());
        dto.setType(event.getType());
        dto.setAmount(event.getAmount());
        dto.setCurrency(event.getCurrency());
        dto.setEventTimestamp(event.getEventTimestamp());

        if (event.getMetadata() != null) {
            try {
                Map<String, Object> metadata =
                        objectMapper.readValue(
                                event.getMetadata(),
                                new TypeReference<>() {
                                }
                        );

                dto.setMetadata(metadata);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(
                        "Unable to read stored metadata"
                );
            }
        }

        return dto;
    }
}
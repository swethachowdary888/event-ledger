package com.eventledger.gateway.service;

import com.eventledger.gateway.dto.EventDto;

public record CreateEventResult(
        EventDto event,
        boolean duplicate
) {
}
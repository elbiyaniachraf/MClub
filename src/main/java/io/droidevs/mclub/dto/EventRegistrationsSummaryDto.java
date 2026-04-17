package io.droidevs.mclub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class EventRegistrationsSummaryDto {
    private UUID eventId;
    private long count;
}

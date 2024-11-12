package com.datastream.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    private String eventType;
    private String source;
    private Map<String, Object> payload;

    // Additional fields for tracking
    private String processingStatus;
    private Long processingTimeMs;

    // Helper method to create events with current timestamp
    public static Event createEvent(String eventType, String source, Map<String, Object> payload) {
        return Event.builder()
                .id(UUID.randomUUID())
                .timestamp(Instant.now())
                .eventType(eventType)
                .source(source)
                .payload(payload)
                .build();
    }
}
package com.acme.api.asynctaskqueue.jobs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public record JobStatusResponse(
        String status,
        int attempts,

        // Optional fields
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String lastError,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant startedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant completedAt
) {}

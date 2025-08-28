package com.acme.api.asynctaskqueue.jobs.dto;

import java.util.Map;

public record JobRequest(
        String type,
        Map<String, Object> payload,
        String idempotencyKey
) {}

package com.acme.api.asynctaskqueue.model;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Job {
    private final String jobId;
    private final String type;
    private final Object payload;
    private final String idempotencyKey;

    private JobStatus status;
    private final AtomicInteger attempts = new AtomicInteger(0);
    private String lastError;
    private Instant startedAt;
    private Instant completedAt;

    public Job(String type, Object payload, String idempotencyKey) {
        this.jobId = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.idempotencyKey = idempotencyKey;
        this.status = JobStatus.QUEUED;
    }

    public String getJobId() {
        return jobId;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts.get();
    }

    public void incrementAttempts() {
        attempts.incrementAndGet();
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}

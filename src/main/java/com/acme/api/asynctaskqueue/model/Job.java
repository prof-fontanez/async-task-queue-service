package com.acme.api.asynctaskqueue.model;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A regular Java POJO responsible to hold a job's state. Purposely designed for mutability
 * since this is a lifecycle object, and the job status will change over time.
 */
public class Job {
    private final String jobId;
    private final String type;
    private final Map<String, Object> payload;
    private final String idempotencyKey;

    private JobStatus status;
    private final AtomicInteger attempts = new AtomicInteger(0);
    private String lastError;
    private Instant startedAt;
    private Instant completedAt;

    public Job(String jobId, String type, Map<String, Object> payload, String idempotencyKey) {
        this.jobId = jobId;
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

    public Map<String, Object> getPayload() {
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

    public int incrementAttempts() {
        return attempts.incrementAndGet();
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

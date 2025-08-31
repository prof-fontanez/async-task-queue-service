package com.acme.api.asynctaskqueue.metrics;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobMetrics {
    private final AtomicLong totalJobExecutionTimeMs = new AtomicLong(0);
    private final AtomicInteger completedJobs = new AtomicInteger(0);

    public void recordJobDuration(long durationMs) {
        totalJobExecutionTimeMs.addAndGet(durationMs);
        completedJobs.incrementAndGet();
    }

    public long getAverageJobTimeMs() {
        int count = completedJobs.get();
        return count == 0 ? 0 : totalJobExecutionTimeMs.get() / count;
    }

    public int getCompletedJobs() {
        return completedJobs.get();
    }
}

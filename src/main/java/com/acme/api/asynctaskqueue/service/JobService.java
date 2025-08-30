package com.acme.api.asynctaskqueue.service;

import com.acme.api.asynctaskqueue.jobs.dto.JobRequest;
import com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse;
import com.acme.api.asynctaskqueue.model.*;
import com.acme.api.asynctaskqueue.repo.JobRepository;
import com.acme.api.asynctaskqueue.worker.JobHandler;
import com.acme.api.asynctaskqueue.worker.JobHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * This is the class that manages the {@link Job} object lifecycle. It manages all updates of the object's
 * state according to the rules of execution, retries, and compensation.
 * <p/>
 * Job objects are created with an initial (default) state of QUEUED. Afterward, this service class will
 * update the status of the job object to one of the appropriate states declared in the {@link JobStatus}
 * enumeration {<i>RUNNING, FAILED, SUCCEEDED, COMPENSATED, and COMPENSATION_FAILED</i>}.
 * <p/>
 * Depending on the job resulting job status once the job is queued, this class will handle the job as
 * follows:
 * <br/><br/>
 * <ul>
 *     <li>All initial jobs are enqueued in the normal queue</li>
 *     <li>All failed and are eligible for retries are enqueued in the retry (single-threaded) queue (Backoff with Jitter)</li>
 *     <li>Jobs that ultimately fail are placed in the compensation queue</li>
 * </ul>
 *
 */
@Service
public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final JobRepository repo;
    private final ThreadPoolExecutor normalExecutor;
    private final ThreadPoolExecutor compensationExecutor;
    private final ScheduledExecutorService retryScheduler;
    private final JobHandlerRegistry handlers;
    private final ConcurrentMap<String, String> idemIndex = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 500L; // 0.5s, doubles each retry

    public JobService(JobRepository repo, @Qualifier("normalJobExecutor") ThreadPoolExecutor normalExecutor,
                      @Qualifier("compensationJobExecutor") ThreadPoolExecutor compensationExecutor,
                      ScheduledExecutorService retryScheduler, JobHandlerRegistry handlers) {
        this.repo = repo;
        this.normalExecutor = normalExecutor;
        this.compensationExecutor = compensationExecutor;
        this.retryScheduler = retryScheduler;
        this.handlers = handlers;
    }

    public Job submitJob(JobRequest req) {
        if (req.idempotencyKey() != null) {
            String existingId = idemIndex.get(req.idempotencyKey());
            if (existingId != null) {
                logger.info("Duplicate submission detected for idempotency key {}. Returning existing job {}", req.idempotencyKey(), existingId);
                return repo.findById(existingId);
            }
        }

        Job job = new Job(UUID.randomUUID().toString(), req.type(), req.payload(), req.idempotencyKey());

        // Put idempotency mapping eagerly
        if (job.getIdempotencyKey() != null) {
            idemIndex.putIfAbsent(job.getIdempotencyKey(), job.getJobId());
        }

        repo.save(job);

        try {
            enqueue(job); // may throw RejectedExecutionException -> controller returns 429
        } catch (RejectedExecutionException rex) {
            logger.warn("Job {} rejected due to backpressure (queue full)", job.getJobId(), rex);
            throw rex;
        }

        logger.info("Job {} submitted successfully (type: {}, idempotencyKey: {})", job.getJobId(), job.getType(), job.getIdempotencyKey());
        return job;
    }

    private void enqueue(Job job) {
        normalExecutor.execute(() -> runOnce(job.getJobId()));
    }

    private void runOnce(String jobId) {
        Job job = repo.findById(jobId);
        if (job == null) return;

        logger.info("Execution started for Job {}", jobId);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Optional.ofNullable(job.getStartedAt()).orElse(Instant.now()));
        repo.save(job);

        JobHandler handler = handlers.get(job.getType());
        try {
            handler.execute(job.getPayload());

            job.setStatus(JobStatus.SUCCEEDED);
            job.setCompletedAt(Instant.now());
            repo.save(job);

            logger.info("Execution SUCCEEDED for Job {}", jobId);

        } catch (Exception ex) {
            job.setLastError(ex.getMessage());
            int retryCount = job.incrementAttempts();
            repo.save(job);

            logger.warn("Execution FAILED for Job {} on attempt {}: {}", jobId, retryCount, ex.getMessage());

            if (retryCount < MAX_ATTEMPTS) {
                long delay = backoffWithJitter(retryCount);
                logger.info("Scheduling retry {} for Job {} in {}ms", retryCount, jobId, delay);

                retryScheduler.schedule(() -> {
                    try {
                        enqueue(Objects.requireNonNull(repo.findById(jobId)));
                    } catch (RejectedExecutionException rex) {
                        logger.warn("Retry for Job {} rejected by scheduler (queue full)", jobId, rex);
                    }
                }, delay, TimeUnit.MILLISECONDS);
            } else {
                // compensation
                job.setStatus(JobStatus.FAILED);
                repo.save(job);

                logger.error("Max attempts reached for Job {}. Triggering compensation.", jobId);

                compensationExecutor.execute(() -> {
                    logger.info("Compensation started for Job {}", jobId);
                    try {
                        handler.compensate(Map.of(
                                "type", job.getType(),
                                "payload", job.getPayload(),
                                "jobId", job.getJobId()
                        ));
                        job.setStatus(JobStatus.COMPENSATED);
                        logger.info("Job {} COMPENSATED", jobId);
                    } catch (Exception cx) {
                        String lastKnownError = job.getLastError() == null ? "UNKNOWN" : job.getLastError();
                        job.setStatus(JobStatus.COMPENSATION_FAILED);
                        job.setLastError(lastKnownError + " | " + "compensation: " + cx.getMessage());
                        logger.error("Compensation FAILED for Job {}. Last known error: {}", jobId, lastKnownError, cx);
                    } finally {
                        job.setCompletedAt(Instant.now());
                        repo.save(job);
                        logger.info("Job {} final status: {}", jobId, job.getStatus());
                    }
                });
            }
        }
    }

    private long backoffWithJitter(int attemptNumber) {
        long exp = (long) (BASE_DELAY_MS * Math.pow(2, attemptNumber - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, 250);
        logger.debug("Backoff with jitter for attempt {}: {} + {}ms = {}ms", attemptNumber, exp, jitter, exp + jitter);
        return exp + jitter;
    }

    public JobStatusResponse getJobStatus(String id) {
        Job j = repo.findById(id);
        if (j == null) throw new IllegalArgumentException("Job not found");
        return new JobStatusResponse(j.getStatus().name(), j.getAttempts(), j.getLastError(), j.getStartedAt(), j.getCompletedAt());
    }
}

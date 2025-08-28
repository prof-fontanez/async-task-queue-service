package com.acme.api.asynctaskqueue.service;

import com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse;
import com.acme.api.asynctaskqueue.model.Job;
import com.acme.api.asynctaskqueue.model.JobStatus;
import com.acme.api.asynctaskqueue.repo.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class JobService {

    private final JobRepository jobRepo;
    private final ThreadPoolExecutor executor;

    public JobService(JobRepository jobRepo, ThreadPoolExecutor executor) {
        this.jobRepo = jobRepo;
        this.executor = executor;
    }

    public void submitJob(Job job) {
        // save first so status can be queried immediately
        jobRepo.save(job);

        try {
            executor.execute(() -> runJob(job));
        } catch (RejectedExecutionException e) {
            throw new RuntimeException("Job queue is full. Please try again later.");
        }
    }

    private void runJob(Job job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        try {
            // simulate job processing
            processJob(job);
            job.setStatus(JobStatus.SUCCEEDED);
        } catch (Exception e) {
            job.incrementAttempts();
            job.setLastError(e.getMessage());

            if (job.getAttempts() < 3) {
                // retry up to 3 times
                submitJob(job);
            } else {
                job.setStatus(JobStatus.FAILED);
                // TODO: implement compensation later (until I understand how it's used or how it works)
            }
        } finally {
            job.setCompletedAt(Instant.now());
        }
    }

    private void processJob(Job job) {
        try {
            // 3 seconds per job to simulate processing time
            // and give time for the queue to get full
            // in order to simulate the condition where 429 is sent back
            Thread.sleep(3000);
            job.setStatus(JobStatus.SUCCEEDED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.setStatus(JobStatus.FAILED);
            job.setLastError(e.getMessage());
        }
    }

    public JobStatusResponse getJobStatus(String jobId) {
        Job job = jobRepo.findById(jobId);
        if (job == null) throw new RuntimeException("Job not found");

        return new JobStatusResponse(
                job.getStatus().name(),
                job.getAttempts(),
                job.getLastError(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}

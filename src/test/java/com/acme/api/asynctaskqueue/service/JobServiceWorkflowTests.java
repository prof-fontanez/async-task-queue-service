package com.acme.api.asynctaskqueue.service;

import com.acme.api.asynctaskqueue.jobs.dto.JobRequest;
import com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse;
import com.acme.api.asynctaskqueue.model.Job;
import com.acme.api.asynctaskqueue.model.JobStatus;
import com.acme.api.asynctaskqueue.repo.JobRepository;
import com.acme.api.asynctaskqueue.worker.JobHandler;
import com.acme.api.asynctaskqueue.worker.JobHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JobServiceWorkflowTests {

    private JobRepository repo;
    private JobHandlerRegistry registry;
    private ThreadPoolExecutor normalExecutor;
    private ThreadPoolExecutor compensationExecutor;
    private ScheduledExecutorService retryScheduler;
    private JobService service;

    // in-memory store to simulate repository
    private ConcurrentMap<String, Job> jobsMap;

    @BeforeEach
    void setup() {
        jobsMap = new ConcurrentHashMap<>();

        repo = mock(JobRepository.class);
        registry = mock(JobHandlerRegistry.class);
        normalExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        compensationExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        retryScheduler = Executors.newSingleThreadScheduledExecutor();

        // mock save() to store jobs in jobsMap
        doAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            jobsMap.put(j.getJobId(), j);
            return null; // save() is void
        }).when(repo).save(any(Job.class));

        // mock findById() to return jobs from jobsMap
        when(repo.findById(anyString())).thenAnswer(invocation -> jobsMap.get(invocation.getArgument(0)));

        service = new JobService(repo, normalExecutor, compensationExecutor, retryScheduler, registry);
    }

    @Test
    void testSuccessfulJobExecution() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        when(registry.get("EMAIL")).thenReturn(handler);

        JobRequest req = new JobRequest("EMAIL", Map.of("to", "user@test.com"), null);
        Job job = service.submitJob(req);

        // Wait for async execution to complete
        waitForJobCompletion(job.getJobId(), 3000);

        assertEquals(JobStatus.SUCCEEDED, job.getStatus());
        verify(handler, times(1)).execute(req.payload());
    }

    @Test
    void testRetryWithJitter() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        doThrow(new RuntimeException("fail1"))
                .doThrow(new RuntimeException("fail2"))
                .doNothing()
                .when(handler).execute(any());
        when(registry.get("REPORT")).thenReturn(handler);

        JobRequest req = new JobRequest("REPORT", Map.of("reportId", 42), null);
        Job job = service.submitJob(req);

        // Wait for retries + success
        waitForJobCompletion(job.getJobId(), 4000);

        assertEquals(JobStatus.SUCCEEDED, job.getStatus());
        assertTrue(job.getAttempts() >= 2);
        verify(handler, atLeast(3)).execute(any());
    }

    @Test
    void testCompensationTriggeredAfterMaxRetries() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        doThrow(new RuntimeException("always fail")).when(handler).execute(any());
        doNothing().when(handler).compensate(any());
        when(registry.get("REPORT")).thenReturn(handler);

        JobRequest req = new JobRequest("REPORT", Map.of("reportId", 99), null);
        Job job = service.submitJob(req);

        // Wait for retries + compensation
        waitForJobCompletion(job.getJobId(), 5000);

        assertEquals(JobStatus.COMPENSATED, job.getStatus());
        verify(handler, atLeast(3)).execute(any());
        verify(handler, times(1)).compensate(any());
    }

    @Test
    void testCompensationFailure() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        doThrow(new RuntimeException("always fail")).when(handler).execute(any());
        doThrow(new RuntimeException("compensation failed")).when(handler).compensate(any());
        when(registry.get("EMAIL")).thenReturn(handler);

        JobRequest req = new JobRequest("EMAIL", Map.of("to", "fail@test.com"), null);
        Job job = service.submitJob(req);

        // Wait for retries + failed compensation
        waitForJobCompletion(job.getJobId(), 5000);

        assertEquals(JobStatus.COMPENSATION_FAILED, job.getStatus());
        assertTrue(job.getLastError().contains("compensation failed"));
    }

    @Test
    void testDuplicateIdempotencyKey() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        when(registry.get("EMAIL")).thenReturn(handler);

        String key = "idempotent-key-123";
        JobRequest req1 = new JobRequest("EMAIL", Map.of("to", "first@test.com"), key);
        JobRequest req2 = new JobRequest("EMAIL", Map.of("to", "second@test.com"), key);

        Job job1 = service.submitJob(req1);

        // Wait for async execution to complete
        waitForJobCompletion(job1.getJobId(), 3000);

        Job job2 = service.submitJob(req2);

        assertEquals(job1.getJobId(), job2.getJobId()); // same job returned

        // Verify that the handler was executed once with first payload
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(handler, times(1)).execute(captor.capture());
        assertEquals("first@test.com", captor.getValue().get("to"));
    }

    @Test
    void testGetJobStatus() {
        Job job = new Job("job-6", "EMAIL", Map.of("to", "test@test.com"), null);
        job.setStatus(JobStatus.SUCCEEDED);
        job.setCompletedAt(Instant.now());

        jobsMap.put("job-6", job);

        JobStatusResponse resp = service.getJobStatus("job-6");

        assertEquals("SUCCEEDED", resp.status());
        assertEquals(0, resp.attempts());
        assertNull(resp.lastError());
    }

    // utility: wait until job status is terminal
    private void waitForJobCompletion(String jobId, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            Job job = jobsMap.get(jobId);
            if (job != null && (
                    job.getStatus() == JobStatus.SUCCEEDED ||
                            job.getStatus() == JobStatus.COMPENSATED ||
                            job.getStatus() == JobStatus.COMPENSATION_FAILED)) {
                return;
            }
            Thread.sleep(50);
        }
    }
}

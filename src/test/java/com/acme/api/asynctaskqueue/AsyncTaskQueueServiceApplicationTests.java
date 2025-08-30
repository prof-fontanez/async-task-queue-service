package com.acme.api.asynctaskqueue;

import com.acme.api.asynctaskqueue.model.Job;
import com.acme.api.asynctaskqueue.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for core Job lifecycle and behaviors.
 */
@SpringBootTest
class AsyncTaskQueueServiceApplicationTests {

	private Job job;

	@BeforeEach
	void setUp() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("email", "user@example.com");
		job = new Job("job-123", "EMAIL", payload, "idem-123");
	}

	@Test
	void contextLoads() {
		// Ensures Spring context starts successfully
	}

	@Test
	void testInitialJobState() {
		assertEquals("job-123", job.getJobId());
		assertEquals("EMAIL", job.getType());
		assertEquals("idem-123", job.getIdempotencyKey());
		assertEquals(JobStatus.QUEUED, job.getStatus());
		assertEquals(0, job.getAttempts());
		assertNull(job.getLastError());
		assertNull(job.getStartedAt());
		assertNull(job.getCompletedAt());
	}

	@Test
	void testIncrementAttempts() {
		int attempt1 = job.incrementAttempts();
		int attempt2 = job.incrementAttempts();

		assertEquals(1, attempt1);
		assertEquals(2, attempt2);
		assertEquals(2, job.getAttempts());
	}

	@Test
	void testSetStatusToInProgress() {
		job.setStatus(JobStatus.RUNNING);
		assertEquals(JobStatus.RUNNING, job.getStatus());
	}

	@Test
	void testSetAndRetrieveError() {
		job.setLastError("SMTP connection failed");
		assertEquals("SMTP connection failed", job.getLastError());
	}

	@Test
	void testStartAndCompleteTimestamps() {
		Instant start = Instant.now();
		job.setStartedAt(start);

		Instant end = start.plusSeconds(60);
		job.setCompletedAt(end);

		assertEquals(start, job.getStartedAt());
		assertEquals(end, job.getCompletedAt());
	}

	@Test
	void testJobFailureAndRetryFlow() {
		// Simulate a failed attempt
		job.incrementAttempts();
		job.setStatus(JobStatus.FAILED);
		job.setLastError("Temporary network issue");

		assertEquals(1, job.getAttempts());
		assertEquals(JobStatus.FAILED, job.getStatus());
		assertEquals("Temporary network issue", job.getLastError());

		// Retry after failure
		job.incrementAttempts();
		job.setStatus(JobStatus.RUNNING);
		job.setLastError(null);

		assertEquals(2, job.getAttempts());
		assertEquals(JobStatus.RUNNING, job.getStatus());
		assertNull(job.getLastError());
	}
}

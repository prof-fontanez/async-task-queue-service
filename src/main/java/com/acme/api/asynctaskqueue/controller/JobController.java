package com.acme.api.asynctaskqueue.controller;

import com.acme.api.asynctaskqueue.jobs.dto.JobRequest;
import com.acme.api.asynctaskqueue.jobs.dto.JobResponse;
import com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse;
import com.acme.api.asynctaskqueue.model.Job;
import com.acme.api.asynctaskqueue.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * Spring MVC controller that exposes the job-related APIs to clients over HTTP. Spring Boot automatically
 * starts an embedded web server (in this case, Tomcat which is the default). Once that occurs, clients can
 * send HTTP requests to this service. For example:
 * <br/>
 * GET http://localhost:8080/v1/jobs/12345
 * <br/>
 * POST http://localhost:8080/v1/jobs
 */
@RestController
@RequestMapping("/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody JobRequest request) {
        try {
            Job job = jobService.submitJob(request);
            return ResponseEntity.ok(new JobResponse(job.getJobId()));
        } catch (RejectedExecutionException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Job queue is full. Please try again later."));
        } catch (IllegalArgumentException badType) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", badType.getMessage()));
        }
    }

    @GetMapping("/{jobId}")
    public JobStatusResponse getStatus(@PathVariable String jobId) {
        return jobService.getJobStatus(jobId);
    }
}

package com.acme.api.asynctaskqueue.controller;

import com.acme.api.asynctaskqueue.jobs.dto.JobRequest;
import com.acme.api.asynctaskqueue.jobs.dto.JobResponse;
import com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse;
import com.acme.api.asynctaskqueue.model.Job;
import com.acme.api.asynctaskqueue.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public JobResponse submitJob(@RequestBody JobRequest request) {
        Job job = new Job(
                request.type(),
                request.payload() != null ? request.payload().toString() : null,
                request.idempotencyKey()
        );

        try {
            jobService.submitJob(job);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        }

        return new JobResponse(job.getJobId());
    }

    @GetMapping("/{jobId}")
    public JobStatusResponse getStatus(@PathVariable String jobId) {
        return jobService.getJobStatus(jobId);
    }
}

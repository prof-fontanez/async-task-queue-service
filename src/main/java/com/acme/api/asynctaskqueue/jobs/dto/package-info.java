/**
 * This package contains classes related to request and response messages. These messages represent an instant in time.
 * Therefore, these Data Transfer Objects (DTOs) are designed for immutability. The main advantage to this is thread-safety
 * in case these message objects need to be shared across threads.
 *
 * <p>It includes:
 * <ul>
 *   <li>{@link com.acme.api.asynctaskqueue.jobs.dto.JobRequest} - Immutable class that represents a POST request</li>
 *   <li>{@link com.acme.api.asynctaskqueue.jobs.dto.JobResponse} - Immutable class that represents a GET request</li>
 *   <li>{@link com.acme.api.asynctaskqueue.jobs.dto.JobStatusResponse} - Immutable class that contains the async
 *   response from a job</li>
 * </ul>
 */
package com.acme.api.asynctaskqueue.jobs.dto;

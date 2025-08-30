package com.acme.api.asynctaskqueue.worker;

import java.util.Map;

/**
 * Each job type must provide execute + compensate.
 */
public interface JobHandler {
    /**
     * Execute the job. Throw an exception to indicate a failure (will be retried).
     * Return value can be ignored; use exceptions for failure paths.
     */
    void execute(Map<String, Object> payload) throws Exception;

    /**
     * Compensate (undo/mitigate) the job after final failure.
     * Never throw here. Log errors so the caller can mark COMPENSATION_FAILED if needed.
     */
    void compensate(Map<String, Object> lastKnownState);
}

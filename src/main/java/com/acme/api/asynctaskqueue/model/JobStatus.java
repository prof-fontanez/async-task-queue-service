package com.acme.api.asynctaskqueue.model;

public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    COMPENSATED
}

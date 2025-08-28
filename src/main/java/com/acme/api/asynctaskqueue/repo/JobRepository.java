package com.acme.api.asynctaskqueue.repo;

import com.acme.api.asynctaskqueue.model.Job;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class JobRepository {

    private final Map<String, Job> store = new ConcurrentHashMap<>();

    public Job findById(String id) {
        return store.get(id);
    }

    public void save(Job job) {
        store.put(job.getJobId(), job);
    }
}

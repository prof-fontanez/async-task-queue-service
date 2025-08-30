package com.acme.api.asynctaskqueue.worker;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobHandlerRegistry {
    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public void register(String type, JobHandler handler) {
        handlers.put(type, handler);
    }

    public JobHandler get(String type) {
        JobHandler h = handlers.get(type);
        if (h == null) throw new IllegalArgumentException("No handler for type: " + type);
        return h;
    }
}

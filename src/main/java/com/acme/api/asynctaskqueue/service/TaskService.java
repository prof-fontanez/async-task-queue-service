package com.acme.api.asynctaskqueue.service;

import com.acme.api.asynctaskqueue.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private Long counter = 1L;

    // Get all tasks
    public List<Task> findAll() {
        return tasks;
    }

    // Find by ID
    public Optional<Task> findById(Long id) {
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    // Create new task
    public Task create(Task task) {
        task.setId(counter++);
        task.setStatus("PENDING");
        tasks.add(task);
        return task;
    }

    // Update task
    public Optional<Task> update(Long id, Task updatedTask) {
        return findById(id).map(task -> {
            task.setName(updatedTask.getName());
            task.setStatus(updatedTask.getStatus());
            return task;
        });
    }

    // Delete task
    public boolean delete(Long id) {
        return tasks.removeIf(t -> t.getId().equals(id));
    }
}

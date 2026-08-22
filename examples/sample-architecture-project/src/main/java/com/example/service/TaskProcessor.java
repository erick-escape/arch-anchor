package com.example.service;

import com.example.dependencies.x.DependencyX;
import com.example.dependencies.y.DependencyY;

/**
 * TaskProcessor - Processes batch tasks
 * Dependencies: {X, Y}
 * Expected Similarity: 0.75 (high similarity with BatchWorker, low with ServiceHandler)
 * Expected Violations: varies
 */
public class TaskProcessor {
    private DependencyX dependencyX;
    private DependencyY dependencyY;

    public void processTask(Object task) {
        // Uses dependencies X, Y
    }

    public void retryTask(Object task) {
        // Retry failed task
    }

    public void completeTask(String taskId) {
        // Mark task as complete
    }
}

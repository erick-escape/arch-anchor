package com.example.service;

import com.example.dependencies.a.DependencyA;
import com.example.dependencies.c.DependencyC;

/**
 * RequestManager - Manages service requests
 * Dependencies: {A, C}
 * Expected Similarity: 0.66
 * Expected Violations: 0
 */
public class RequestManager {
    private DependencyA dependencyA;
    private DependencyC dependencyC;

    public void queueRequest(Object request) {
        // Uses dependencies A, C
    }

    public Object getNextRequest() {
        // Retrieve next request
        return new Object();
    }

    public void cancelRequest(String requestId) {
        // Cancel pending request
    }
}

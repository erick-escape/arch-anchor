package com.example.service;

import com.example.dependencies.b.DependencyB;
import com.example.dependencies.c.DependencyC;
import com.example.dependencies.d.DependencyD;

/**
 * DataValidator - Validates service data
 * Dependencies: {B, C, D}
 * Expected Similarity: 0.64
 * Expected Violations: 6 (D is not in reference class dependencies)
 */
public class DataValidator {
    private DependencyB dependencyB;
    private DependencyC dependencyC;
    private DependencyD dependencyD;

    public boolean validate(Object data) {
        // Uses dependencies B, C, D
        return true;
    }

    public String[] getValidationErrors(Object data) {
        // Get validation errors
        return new String[0];
    }

    public void configureValidationRules(String rules) {
        // Configure validation
    }
}

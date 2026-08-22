package com.example.database;

import com.example.dependencies.w.DependencyW;
import com.example.dependencies.x.DependencyX;
import com.example.dependencies.y.DependencyY;

/**
 * UIRenderer - MISFIT CLASS - Should be in Presentation module
 * Dependencies: {W, X, Y}
 * Expected Similarity: 0.05 (very low - doesn't fit database module)
 * Expected Violations: 12
 *
 * This class demonstrates the MoveClass scenario - it belongs in the
 * presentation module because its dependencies {W, X, Y} match that module,
 * not the database module's {P, Q, R} dependencies.
 */
public class UIRenderer {
    private DependencyW dependencyW;
    private DependencyX dependencyX;
    private DependencyY dependencyY;

    public void renderUI(Object data) {
        // Uses dependencies W, X, Y (presentation dependencies)
        // This is a misfit in the database module
    }

    public void updateDisplay(String content) {
        // UI update logic
    }

    public void refreshView() {
        // View refresh logic
    }
}

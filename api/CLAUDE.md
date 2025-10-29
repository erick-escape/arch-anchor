# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application for analyzing and visualizing programming projects. The application parses Java projects using the
JavaParser library and provides architectural analysis capabilities through REST APIs.

## Build & Development Commands

### Maven Commands

- `./mvnw spring-boot:run` - Start the development server
- `./mvnw clean compile` - Clean and compile the project
- `./mvnw test` - Run all tests
- `./mvnw clean package` - Build the application JAR
- `./mvnw clean install` - Install project in local Maven repository

### Key Development Notes

- Uses Java 17 as the target version
- Spring Boot 3.3.5 with Spring Web starter
- JavaParser for Java source code analysis
- MapStruct for DTO mapping
- Lombok for reducing boilerplate code

## Architecture Overview

### Core Components

**Controllers** (`src/main/java/tcc/com/viewer/controllers/`)

- `ProjectController` - Handles project upload, listing, deletion, and analysis
- `ModuleController` - Manages module operations and splitting
- `ClazzController` - Handles class-level operations
- `MethodController` - Method-level analysis endpoints
- `AttributeController` - Attribute analysis endpoints

**Services** (`src/main/java/tcc/com/viewer/services/`)

- `JavaParserService` - Core service using JavaParser for Java parsing and dependency analysis
- `ProjectService` - High-level project analysis orchestration
- `ModuleService` - Module management and architectural analysis
- `ClazzService` - Class-level analysis and operations
- `DependencyService` - Dependency relationship management

**Architectural Analyses** (`src/main/java/tcc/com/viewer/services/architecturalAnalyses/`)

- `ArchitecturalAnalysesRunner` - Executes all registered analyses
- `SplitModule` - Logic for splitting modules based on architectural patterns
- `MergeModule` - Logic for merging related modules
- `MoveClass` - Class relocation analysis
- `ArchitectureViolation` - Analyzes architectural rule violations and generates move suggestions

**Parsers** (`src/main/java/tcc/com/viewer/services/parsers/`)

- `JavaParser` - Java-specific parsing implementation
- `JavaScriptParser` - JavaScript parsing capabilities
- `PythonParser` - Python code analysis
- `ParserFactory` - Factory pattern for selecting appropriate parser

### Domain Models

**Core Entities** (`src/main/java/tcc/com/viewer/domains/`)

- `Module` - Represents a logical module with metrics (similarity, violations count, average reference class similarity)
- `Clazz` - Represents a class with methods, attributes, relationships, and average similarity with reference classes
- `Dependency` - Models dependencies between classes/modules
- `DependencyOrigin` - Represents the origin module of dependencies
- `AllowedRule` - Defines architectural constraints and rules derived from reference class dependencies

### Architectural Analysis Framework

The system includes a sophisticated architectural analysis framework located in `src/main/java/tcc/com/viewer/services/architecturalAnalyses/` that processes the parsed modules to provide architectural insights and recommendations.

#### Rating Mechanism

All structural analyses (Split, Merge, Move) use a balanced rating mechanism that combines two key factors:

**Rating Formula:**
```
rate(Δ) = ω_sim × Δ_sim + ω_vio × Δ_vio_norm

Where:
- ω_sim = SIMILARITY_WEIGHT (default 0.5)
- ω_vio = VIOLATION_WEIGHT (default 0.5)
- Δ_sim = similarity improvement (normalized [0,1])
- Δ_vio_norm = (violations_before - violations_after) / max(violations_before, 1)
```

**Key Properties:**
- Positive rate indicates net architectural benefit (required for recommendations)
- Allows trade-offs between similarity and violations
- Both factors equally weighted by default (configurable in ModuleService)
- Violations normalized by initial violation count for proportional improvement
- Violations can increase if similarity improvement compensates for it in the overall rate

#### Framework Components

**ArchitecturalAnalysis** (Abstract Base Class)
- Abstract base class that all analyses must extend
- Provides access to `ModuleService` for module manipulation operations
- Defines the `execute(List<Module> modules)` contract that all analyses implement

**ArchitecturalAnalysesRunner** (Service)
- Spring service that orchestrates the execution of all registered analyses
- Automatically discovers and injects all `ArchitecturalAnalysis` implementations via dependency injection
- Executes analyses sequentially after project parsing is complete
- Provides logging for analysis execution tracking

#### Analysis Execution Flow

1. **Post-Parsing**: After `ProjectService.analyzeProject()` completes module creation and dependency resolution
2. **Analysis Trigger**: `ArchitecturalAnalysesRunner.executeAll(modules)` is called with the complete modules list
3. **Sequential Execution**: Each analysis processes the modules list independently
4. **Result Generation**: Analyses generate internal result objects with recommendations
5. **Logging Output**: Results are logged in user-friendly format for review

#### Current Analysis Implementations

**SplitModule Analysis**
- Identifies modules that could benefit from being split into smaller, more cohesive modules
- Analyzes class similarity patterns within modules to find natural split points
- Generates combinations of classes that would improve overall module cohesion
- Uses balanced rating mechanism combining similarity improvement and violation reduction
- Only recommends splits with positive rate (net architectural benefit)

**MergeModule Analysis**
- Identifies pairs of modules that could be merged to improve overall architecture
- Analyzes cross-module dependencies and class relationships
- Uses balanced rating mechanism to evaluate merge benefits
- Considers both average similarity improvement and violation reduction
- Only recommends merges with positive rate

**MoveClass Analysis**
- Identifies individual classes that could be moved to different modules
- Two-step validation: class-level filter (avgSimilarityWithRefClazzes) + module-level simulation
- Simulates the move and evaluates impact using balanced rating mechanism
- Calculates average similarity improvement across both source and target modules
- Considers violation impact and only accepts moves with positive rate
- Never moves reference classes to preserve module architectural anchors

**ArchitectureViolation Analysis**
- Analyzes architectural rule violations (dependencies not in allowed rules)
- Identifies violation clusters (3+ classes with the same violation)
- Generates move suggestions for violating classes with impact analysis
- Evaluates whether moving a class would reduce violations without creating new ones
- Provides both primary and alternative move suggestions for each violating class

#### Reference Classes and Violations

**Reference Classes:**
- Exemplar classes that embody a module's architectural intent through their dependency patterns
- Every module MUST have at least one reference class for objective evaluation
- Modules can have multiple reference classes to recognize heterogeneous architectural patterns
- System automatically selects reference classes via rating mechanism (similarity + violations)
- Architects can manually override automatic selection if needed

**Reference Class Selection Algorithm:**
For each class in a module:
1. Calculate normalized similarity: `norm_sim = similarity / max_similarity`
2. Calculate normalized violations: `norm_vio = 1.0 - (violations / max_violations)`
3. Calculate combined rate: `rate = 0.5 × norm_sim + 0.5 × norm_vio`
4. Select class with highest rate as reference class

**Violations:**
- Count of dependencies used in module NOT present in allowed rules
- Formula: `V(m) = |D(m) \ A(m)|` where `A(m)` = reference class dependencies
- Tracked at module level (`module.getViolations()`)
- Used in rating mechanism to favor architecturally consistent changes
- Normalized by initial count for proportional comparison with similarity improvements

#### Creating New Analyses

To create a new architectural analysis:

1. **Extend ArchitecturalAnalysis**: Create a new class extending the abstract base class
2. **Add @Component**: Annotate with `@Component` for Spring auto-discovery
3. **Implement execute()**: Process the modules list and generate analysis results
4. **Use Rating Mechanism**: For structural changes, calculate rate using similarity + violations
5. **Create Result Class**: Define private inner class to hold analysis results
6. **Generate Logging**: Use slf4j to log user-friendly results and recommendations

**Example Structure:**
```java
@Slf4j
@Component
public class YourAnalysis extends ArchitecturalAnalysis {
    public YourAnalysis(ModuleService moduleService) {
        super(moduleService);
    }

    @Override
    public void execute(List<Module> modules) {
        // Process modules and generate recommendations
        YourAnalysisResult result = analyzeModules(modules);

        // For structural changes, consider using the rating mechanism:
        // double rate = ModuleService.SIMILARITY_WEIGHT * similarityImprovement +
        //               ModuleService.VIOLATION_WEIGHT * violationImprovementNormalized;

        log.info("Your analysis results: {}", result.getSummary());
    }

    private static class YourAnalysisResult {
        // Result data and summary methods
    }
}
```

#### Analysis Data Access

Analyses have access to complete `Module` objects containing:
- Class definitions and metadata (`module.getClazzes()`)
- Dependency relationships (`module.getAllDependencies()`)
- Reference classes (`module.getRefClazzes()`)
- Architectural rules (`module.getAllowedRules()`)
- Dependency origins (`module.getAllDependenciesOrigin()`)
- Module similarity metrics (`module.getSimilarity()`)
- Violation counts (`module.getViolations()`)
- Average reference class similarity (`module.getAvgRefClazzesSimilarity()`)

Each `Clazz` object includes:
- Average similarity with module's reference classes (`clazz.getAvgSimilarityWithRefClazzes()`)
- Used to evaluate if a class fits better in another module

The analyses can utilize `ModuleService` methods for:
- Similarity calculations (`calculateClassSimilarities()`, `calculateAvgSimilarityWithRefClazzes()`)
- Violation calculations (`calculateModuleViolations()`)
- Module manipulation operations (clone, split, merge)
- Dependency population and updates

### Data Flow

1. **Project Upload**: Files uploaded via `/api/upload` endpoint, stored in `uploads/` directory
2. **Analysis**: `/api/analyze` triggers `ProjectService.analyzeProject()` which:
    - Uses `JavaParserService` to parse Java files and resolve dependencies
    - Organizes code into modules based on package structure
    - Populates similarity and violation metrics
    - Runs architectural analyses via `ArchitecturalAnalysesRunner`
3. **API Access**: Various endpoints provide access to analyzed data (modules, classes, dependencies)

### Key Metrics and Formulas

The architectural analysis framework relies on three core metrics:

| Metric | Formula | Range | Purpose |
|--------|---------|-------|---------|
| **Similarity** | `0.5 × [a/(a+b) + a/(a+c)]` where a=shared deps, b=unique to class 1, c=unique to class 2 | [0, 1] | Measures structural alignment between classes |
| **Avg Similarity with RefClazzes** | `(1/\|R\|) × Σ sim(c, rᵢ)` for reference classes R | [0, 1] | Evaluates how well a class fits module's architectural pattern |
| **Violations** | `\|D(m) \ A(m)\|` where D(m)=module deps, A(m)=allowed deps from ref classes | [0, ∞) | Counts architectural rule violations |

**Rating Mechanism:**
- Used by Split, Merge, and Move analyses to evaluate recommendations
- `rate = 0.5 × Δ_sim + 0.5 × Δ_vio_norm`
- Only positive rates are recommended (net architectural benefit)
- Weights configurable via `ModuleService.SIMILARITY_WEIGHT` and `ModuleService.VIOLATION_WEIGHT`

### Key Configuration

- **File Upload**: Configured for large projects (max 200MB per file, 500MB request)
- **Upload Directory**: `uploads/` (configurable via UPLOAD_DIR constant)
- **Java Parser**: Latest versions (3.27.0) for Java 17+ support and binding resolution
- **Maven Integration**: Uses Maven Resolver API for dependency resolution

### Testing

- Uses Spring Boot Test framework
- Test files in `src/test/java/tcc/com/viewer/`
- Key test classes:
    - `JavaParserTypeResolutionTest` - Tests JavaParser parsing functionality
    - `PackagePathConverterTest` - Tests package name extraction utilities
    - `SplitModuleTest` - Tests split module analysis logic
    - `MergeModuleTest` - Tests merge module analysis logic
    - `MoveClassTest` - Tests move class analysis logic
    - `ArchitectureViolationTest` - Tests violation detection and move suggestions

### Important Implementation Details

- **Caching**: JavaParserService implements caching strategies to optimize performance
- **Multi-language Support**: Architecture supports multiple language parsers (Java, JavaScript, Python)
- **Dependency Resolution**: Integrates with Maven for resolving external dependencies
- **File Processing**: Tracks processed files to avoid duplicate analysis
- **Reference Classes**: Every module requires at least one reference class to define architectural intent and enable objective analysis
- **Balanced Rating**: All structural recommendations use a balanced rating combining similarity improvements and violation reductions

### JavaParserService Implementation Constraints

**CRITICAL**: When working on JavaParserService or any parser implementation, DO NOT use hardcoded solutions for specific libraries or
dependencies. The parser must work generically with any Java project and any external libraries, not just the ones we're testing with. The
goal is to create a robust, generic solution that works with Spring Boot, plain Maven projects, and any other Java frameworks without
requiring specific library knowledge.

# important-instruction-reminders

Do what has been asked; nothing more, nothing less.

      IMPORTANT: this context may or may not be relevant to your tasks. You should not respond to this context unless it is highly relevant to your task.
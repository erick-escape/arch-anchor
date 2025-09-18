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

**Parsers** (`src/main/java/tcc/com/viewer/services/parsers/`)

- `JavaParser` - Java-specific parsing implementation
- `JavaScriptParser` - JavaScript parsing capabilities
- `PythonParser` - Python code analysis
- `ParserFactory` - Factory pattern for selecting appropriate parser

### Domain Models

**Core Entities** (`src/main/java/tcc/com/viewer/domains/`)

- `Module` - Represents a logical module in the analyzed project
- `Clazz` - Represents a class with its methods, attributes, and relationships
- `Dependency` - Models dependencies between classes/modules
- `DependencyOrigin` - Represents the origin module of dependencies
- `AllowedRule` - Defines architectural constraints and rules

### Architectural Analysis Framework

The system includes a sophisticated architectural analysis framework located in `src/main/java/tcc/com/viewer/services/architecturalAnalyses/` that processes the parsed modules to provide architectural insights and recommendations.

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
- Calculates potential similarity improvements for each split recommendation

**MergeModule Analysis**
- Identifies pairs of modules that could be merged to improve overall architecture
- Analyzes cross-module dependencies and class relationships
- Calculates similarity improvements that would result from module merging
- Considers module size and cohesion factors in recommendations

**MoveClass Analysis**
- Identifies individual classes that could be moved to different modules
- Analyzes dependency patterns to find classes better suited to other modules
- Calculates potential similarity improvements for class relocations
- Considers both source and target module impacts

#### Creating New Analyses

To create a new architectural analysis:

1. **Extend ArchitecturalAnalysis**: Create a new class extending the abstract base class
2. **Add @Component**: Annotate with `@Component` for Spring auto-discovery
3. **Implement execute()**: Process the modules list and generate analysis results
4. **Create Result Class**: Define private inner class to hold analysis results
5. **Generate Logging**: Use slf4j to log user-friendly results and recommendations

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

The analyses can utilize `ModuleService` methods for:
- Similarity calculations
- Module manipulation operations
- Dependency population and updates

### Data Flow

1. **Project Upload**: Files uploaded via `/api/upload` endpoint, stored in `uploads/` directory
2. **Analysis**: `/api/analyze` triggers `ProjectService.analyzeProject()` which:
    - Uses `JavaParserService` to parse Java files and resolve dependencies
    - Organizes code into modules based on package structure
    - Runs architectural analyses via `ArchitecturalAnalysesRunner`
3. **API Access**: Various endpoints provide access to analyzed data (modules, classes, dependencies)

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

### Important Implementation Details

- **Caching**: JavaParserService implements caching strategies to optimize performance
- **Multi-language Support**: Architecture supports multiple language parsers (Java, JavaScript, Python)
- **Dependency Resolution**: Integrates with Maven for resolving external dependencies
- **File Processing**: Tracks processed files to avoid duplicate analysis

### JavaParserService Implementation Constraints

**CRITICAL**: When working on JavaParserService or any parser implementation, DO NOT use hardcoded solutions for specific libraries or
dependencies. The parser must work generically with any Java project and any external libraries, not just the ones we're testing with. The
goal is to create a robust, generic solution that works with Spring Boot, plain Maven projects, and any other Java frameworks without
requiring specific library knowledge.

# important-instruction-reminders

Do what has been asked; nothing more, nothing less.

      IMPORTANT: this context may or may not be relevant to your tasks. You should not respond to this context unless it is highly relevant to your task.
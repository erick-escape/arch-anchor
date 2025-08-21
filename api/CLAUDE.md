# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application for analyzing and visualizing programming projects. The application parses Java projects using Eclipse JDT and provides architectural analysis capabilities through REST APIs.

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
- ANTLR4 for parsing (generates sources during compilation in target/generated-sources/antlr4)
- Eclipse JDT Core for Java source code analysis
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
- `JDTParserService` - Core service using Eclipse JDT for Java parsing and dependency analysis
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
- `AllowedRule` - Defines architectural constraints and rules

### Data Flow

1. **Project Upload**: Files uploaded via `/api/upload` endpoint, stored in `uploads/` directory
2. **Analysis**: `/api/analyze` triggers `ProjectService.analyzeProject()` which:
   - Uses `JDTParserService` to parse Java files and resolve dependencies
   - Organizes code into modules based on package structure
   - Runs architectural analyses via `ArchitecturalAnalysesRunner`
3. **API Access**: Various endpoints provide access to analyzed data (modules, classes, dependencies)

### Key Configuration

- **File Upload**: Configured for large projects (max 200MB per file, 500MB request)
- **Upload Directory**: `uploads/` (configurable via UPLOAD_DIR constant)
- **Eclipse JDT**: Latest versions (3.38.0) for Java 17+ support and binding resolution
- **Maven Integration**: Uses Maven Resolver API for dependency resolution

### Testing

- Uses Spring Boot Test framework
- Test files in `src/test/java/tcc/com/viewer/`
- Key test classes:
  - `JDTTypeResolutionTest` - Tests JDT parsing functionality
  - `PackagePathConverterTest` - Tests package name extraction utilities

### Important Implementation Details

- **Caching**: JDTParserService implements classpath and sourcepath caching to optimize performance
- **Multi-language Support**: Architecture supports multiple language parsers (Java, JavaScript, Python)
- **Dependency Resolution**: Integrates with Maven for resolving external dependencies
- **File Processing**: Tracks processed files to avoid duplicate analysis
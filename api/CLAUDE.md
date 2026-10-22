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

**Controllers** (`src/main/java/com/archanchor/controllers/`)

- `ProjectController` - Handles project upload, listing, deletion, and analysis
- `RepositoryImportController` - Creates a project from a GitHub repository URL (`POST /api/projects/import`)
- `ModuleController` - Manages module operations (merge, split, delete, rename, move class, set ref classes)
- `RecommendationsController` - Returns and applies architectural recommendations (split, merge, move)
- `ClazzController` - Placeholder for class-level operations (currently empty)
- `MethodController` - Placeholder for method-level analysis (currently empty)
- `AttributeController` - Placeholder for attribute analysis (currently empty)

**Services** (`src/main/java/com/archanchor/services/`)

- `JavaParserService` - Core service using JavaParser for Java parsing and dependency analysis
- `ProjectService` - High-level project analysis orchestration
- `ModuleService` - Module management, similarity calculations, violation tracking, and module manipulation
- `RecommendationsService` - Aggregates results from all architectural analyses into recommendation DTOs
- `ClazzService` - Placeholder for class-level operations (currently empty)
- `DependencyService` - Placeholder for dependency relationship management (currently empty)

**Architectural Analyses** (`src/main/java/com/archanchor/services/architecturalAnalyses/`)

- `ArchitecturalAnalysesRunner` - Executes all registered analyses
- `SplitModule` - Logic for splitting modules based on architectural patterns
- `MergeModule` - Logic for merging related modules
- `MoveClass` - Class relocation analysis
- `ArchitectureViolation` - Analyzes architectural rule violations and generates move suggestions

**Parsers** (`src/main/java/com/archanchor/services/parsers/`)

- `JavaParser` - Java-specific parsing implementation
- `JavaScriptParser` - JavaScript parsing capabilities
- `PythonParser` - Python code analysis
- `ParserFactory` - Factory pattern for selecting appropriate parser

### Domain Models

**Core Entities** (`src/main/java/com/archanchor/domains/`)

- `Module` - Represents a logical module with fields: `id`, `name`, `refClazzes` (List<Clazz>), `refClazzesDependencies` (List<Dependency>),
  `moduleDependencies` (List<Dependency>), `clazzes` (List<Clazz>), `similarity` (Double), `avgRefClazzesSimilarity` (Double),
  `violations` (Integer)
- `Clazz` - Represents a class with fields: `id`, `name`, `dependencies` (List<Dependency>), `similarity` (Double),
  `avgSimilarityWithRefClazzes` (Double), `firstModule` (String), `currentModule` (String)
- `Dependency` - Models a dependency on an external package with fields: `packageName` (String), `types` (List<Type>)
- `Type` - Represents a specific type within a dependency with fields: `fullyQualifiedName` (String), `className` (String)

### Architectural Analysis Framework

The system includes a sophisticated architectural analysis framework located in
`src/main/java/com/archanchor/services/architecturalAnalyses/` that processes the parsed modules to provide architectural insights and
recommendations.

#### Rating Mechanism

All structural analyses (Split, Merge, Move) use a rating mechanism that combines two key factors, weighted as the
architect sets for the project:

**Rating Formula:**

```
rate(Δ) = ω_sim × Δ_sim + ω_vio × Δ_vio_norm

Where:
- (ω_sim, ω_vio) = the project's recommendation weights (default 0.5 / 0.5)
- Δ_sim = similarity improvement (normalized [0,1])
- Δ_vio_norm = (violations_before - violations_after) / max(violations_before, 1)
```

**Key Properties:**

- Positive rate indicates net architectural benefit (required for recommendations)
- Allows trade-offs between similarity and violations
- Both factors equally weighted by default; the architect changes the balance per project (see Rating Weights below)
- Violations normalized by initial violation count for proportional improvement
- Violations can increase if similarity improvement compensates for it in the overall rate

#### Framework Components

**ArchitecturalAnalysis** (Abstract Base Class)

- Abstract base class that all analyses must extend
- Provides access to `ModuleService` for module manipulation operations
- Provides `recommendationRate(Δ_sim, Δ_vio_norm)`, which applies the recommendation weights in force (`ActiveRatingWeights`)
- Defines the `execute(List<Module> modules)` contract that all analyses implement

#### Rating Weights

- Two pairs per project (`domains/rating/RatingWeights`): `election`, used by `ModuleService.selectReferenceClasses`, and
  `recommendation`, used by Split, Merge and Move. Each `WeightPair` is non-negative and sums to 1, so the priority
  thresholds of `RecommendationsService` keep their meaning; anything else throws `InvalidRatingWeightsException`
- Stored per project in `rating-weights.json` by `JsonFileRatingWeightsStore` (`RatingWeightsStore`); a project without an
  entry has 0.5 / 0.5, and deleting a project forgets its entry
- `ActiveRatingWeights` holds the weights of the project whose modules are saved, injected into `ModuleService` and every
  analysis. `ProjectService.analyzeProject` adopts the analysed project's weights first
- `PUT /api/projects/{projectName}/rating-weights` saves new weights; for the analysed project,
  `ProjectService.updateRatingWeights` re-elects the saved modules, except those with `refClazzesChosenByHand`, re-runs the
  analyses and saves, so module operations survive
- `BookstoreRunningExampleTest` pins `examples/bookstore` at the default weights; the README and the ICSE 2027 paper quote
  those numbers

#### Analysis Settings

- Per project (`domains/analysis/AnalysisSettings`, #36), deciding what the analyses recommend and show; the defaults
  filter nothing:
    - `minSplitModuleClasses` (1): `SplitModule` rejects a split with a smaller half
    - `minSourceClassesAfterMove` (1): `MoveClass` skips a class whose move would leave its module smaller
    - `maxMoveTargetsPerClass` (null, no limit): `MoveClass` keeps each class's best-rated targets
    - `minClassDependencies` (0): a class with fewer package dependencies gets no move and no violation entry
      (`MoveClass`, `ArchitectureViolation`); module violation counts, and so the rates, still include it
    - `minRecommendationRate` (0): `RecommendationsService` hides splits, merges and moves rated lower, before the summary;
      violation entries have no rate and are always shown
    - `minMergeSimilarityChange` (null, no limit; #41): `MergeModule` does not recommend a merge that changes similarity by
      less; 0 keeps out merges that only remove violations at a cost in similarity
    - `minMoveSimilarityChange` (null, no limit; #73): the same for a move, in `MoveClass` before `bestTargetsOfEachClass`,
      so a class whose best target costs more similarity keeps its best target that does not
    - `minSplitSimilarityChange` (null, no limit; #73): the same for a split, in `SplitModule`
    - A similarity-change limit missing from a stored file or a PUT reads as null, so settings saved before it keep no limit
- Invalid values throw `InvalidAnalysisSettingsException`, naming the setting, the value and the range
- Stored per project in `analysis-settings.json` by `JsonFileAnalysisSettingsStore` (`AnalysisSettingsStore`; the JSON file
  handling is shared with the weights in `util/PerProjectJsonFile`); deleting a project forgets them
- `ActiveAnalysisSettings` holds the settings of the project whose modules are saved, injected into `SplitModule`,
  `MergeModule`, `MoveClass`, `ArchitectureViolation` and `RecommendationsService`; `ProjectService.analyzeProject`
  adopts them
- `PUT /api/projects/{projectName}/analysis-settings` saves them; for the analysed project,
  `ProjectService.updateAnalysisSettings` re-runs the analyses without re-electing, since no setting affects the election

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
5. **API Access**: `RecommendationsService` collects results from all analyses and exposes them via `RecommendationsController`

#### Current Analysis Implementations

**SplitModule Analysis**

- Identifies modules that could benefit from being split into smaller, more cohesive modules
- Analyzes class similarity patterns within modules to find natural split points
- Finds a "super ref class" (highest similarity) and a "de-ref class" (lowest similarity non-reference class) as anchors;
  the de-ref class is chosen among classes with at least one package dependency and the minimum of the analysis settings
  (#44), since a class with none is similar to nothing
- Assigns remaining classes to one of the two new modules based on proximity to the anchor classes
- Uses the rating mechanism, combining similarity improvement and violation reduction with the recommendation weights
- Only recommends splits with positive rate (net architectural benefit)

**MergeModule Analysis**

- Identifies pairs of modules that could be merged to improve overall architecture
- Analyzes cross-module dependencies and class relationships
- Uses the rating mechanism to evaluate merge benefits
- Considers both average similarity improvement and violation reduction
- Only recommends merges with positive rate
- The merged module keeps the reference classes of both modules, in the rating and when the merge is applied
  (`ModuleService.measureWithReferenceClasses`, #34); an applied merge is marked `refClazzesChosenByHand`, so a change
  of election weights does not collapse it to one reference class

**MoveClass Analysis**

- Identifies individual classes that could be moved to different modules
- Two-step validation: class-level filter (avgSimilarityWithRefClazzes) + module-level simulation
- Simulates the move and evaluates impact using the rating mechanism
- Calculates average similarity improvement across both source and target modules
- Considers violation impact and only accepts moves with positive rate
- Never moves reference classes to preserve module architectural anchors

**ArchitectureViolation Analysis**

- Analyzes architectural rule violations (dependencies not in reference class dependencies)
- Identifies violation clusters (3+ classes with the same violation)
- Generates move suggestions for violating classes with impact analysis
- Evaluates whether moving a class would reduce violations without creating new ones
- Provides both primary and alternative move suggestions for each violating class

#### Reference Classes and Violations

**Module scope (#43):**

- A module read from disk never depends on its own classes, because the parser drops same-package references. A class
  moved or merged into a module keeps the references it was parsed with, so `ModuleScope` treats a reference to one of
  the module's own classes (or a type nested in one) as internal: `ModuleService` leaves it out of similarity, module
  and reference-class dependencies, violations and the election, and `MoveClass` and `ArchitectureViolation` judge a
  class moved into a target with `ModuleScope.of(target).with(clazz)`
- Each `Clazz` records the package its source declares (`packageName`, read by `util/PackageDeclaration`), kept in
  `ClazzResponseDTO`; a class of unknown package is never matched

**Reference Classes:**

- Exemplar classes that embody a module's architectural intent through their dependency patterns
- Every module MUST have at least one reference class for objective evaluation
- Modules can have multiple reference classes to recognize heterogeneous architectural patterns
- System automatically selects reference classes via rating mechanism (similarity + violations)
- Architects can manually override automatic selection if needed; the module is then marked `refClazzesChosenByHand`, and
  a change of election weights leaves it alone
- Reference class dependencies are stored in `module.getRefClazzesDependencies()` and define the architectural constraints for the module

**Reference Class Selection Algorithm:**
For each class in a module:

1. Calculate normalized similarity: `norm_sim = similarity / max_similarity`
2. Calculate normalized violations: `norm_vio = 1.0 - (violations / max_violations)`
3. Calculate combined rate with the project's election weights: `rate = ω_sim × norm_sim + ω_vio × norm_vio` (0.5 / 0.5
   by default); when no candidate leaves a violation, similarity alone decides
4. Select class with highest rate as reference class

**Violations:**

- Count of dependencies used in module NOT present in the reference classes' dependencies
- Formula: `V(m) = |D(m) \ R(m)|` where `D(m)` = module dependencies, `R(m)` = reference class dependencies (`refClazzesDependencies`)
- Tracked at module level (`module.getViolations()`)
- Used in rating mechanism to favor architecturally consistent changes
- Normalized by initial count for proportional comparison with similarity improvements

#### Creating New Analyses

To create a new architectural analysis:

1. **Extend ArchitecturalAnalysis**: Create a new class extending the abstract base class
2. **Add @Component**: Annotate with `@Component` for Spring auto-discovery
3. **Implement execute()**: Process the modules list and generate analysis results
4. **Use Rating Mechanism**: For structural changes, calculate rate using similarity + violations
5. **Simulate inside `simulateOn`**: Hypothetical modules share their `Clazz` instances with the real ones, and `ModuleService`
   writes similarity metrics into them. Wrap every what-if computation in `simulateOn(realModules, () -> ...)` so those metrics are
   put back afterwards; otherwise the next analysis and the API response read the simulation's leftovers (#16)
6. **Create Result Class**: Define private inner class to hold analysis results
7. **Expose Results**: Add a `getResults()` method and wire into `RecommendationsService` if results should be accessible via API
8. **Generate Logging**: Use slf4j to log user-friendly results and recommendations

**Example Structure:**

```java

@Slf4j
@Component
public class YourAnalysis extends ArchitecturalAnalysis {
    public YourAnalysis(ModuleService moduleService, ActiveRatingWeights activeWeights) {
        super(moduleService, activeWeights);
    }

    @Override
    public void execute(List<Module> modules) {
        // Process modules and generate recommendations
        YourAnalysisResult result = analyzeModules(modules);

        // For structural changes, consider using the rating mechanism:
        // double rate = recommendationRate(similarityImprovement, violationImprovementNormalized);

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
- Reference classes (`module.getRefClazzes()`)
- Reference class dependencies / architectural constraints (`module.getRefClazzesDependencies()`)
- All module dependencies aggregated (`module.getModuleDependencies()`)
- Module similarity metrics (`module.getSimilarity()`)
- Violation counts (`module.getViolations()`)
- Average reference class similarity (`module.getAvgRefClazzesSimilarity()`)

Each `Clazz` object includes:

- Per-class dependencies (`clazz.getDependencies()`)
- Average similarity with module's reference classes (`clazz.getAvgSimilarityWithRefClazzes()`)
- Used to evaluate if a class fits better in another module

The analyses can utilize `ModuleService` methods for:

- Similarity calculations (`calculateClassSimilarities()`, `calculateAvgSimilarityWithRefClazzes()`)
- Violation calculations (`calculateModuleViolations()`)
- Module manipulation operations (clone, split, merge)
- Dependency population and updates (`populateModuleDependencies()`, `populateRefClazzesDependencies()`)

### Data Flow

1. **Project Upload or Import**: Files uploaded via `/api/upload`, or a GitHub repository shallow-cloned via
   `/api/projects/import` (`services/repositoryImport/`), end up in the same place: `uploads/<project>/`, laid out by
   `ProjectsDirectory`. An import stages in `uploads/.staging/`, strips `.git/` and symbolic links, and requires `src/`
   directly under the chosen root, because `ModuleService.getModules` starts there
2. **Analysis**: `/api/analyze` triggers `ProjectService.analyzeProject()` which:
    - Uses `JavaParserService` to parse Java files and resolve dependencies
    - Organizes code into modules based on package structure: one module per directory with sources, under `src/main`
      when the project has one, so that tests are not modules (#47), and under `src` otherwise
    - Populates similarity and violation metrics
    - Runs architectural analyses via `ArchitecturalAnalysesRunner`
    - Returns `ProjectAnalysesDTO` with modules and architectural constraints
3. **Recommendations API**: `GET /api/recommendations` aggregates results from all analyses into `RecommendationsResponseDTO`
4. **Apply Recommendations**: `POST /api/recommendations/apply/{split|merge|move}` applies a selected recommendation to the saved
   modules, then re-runs the analyses on them (`ProjectService.refreshRecommendations`), so `GET /api/recommendations` describes the
   new structure. The page reloads the modules with `GET /api/module`; re-analysing would discard the change, because module
   operations are never written back to the sources
5. **Module Operations**: `/api/module/*` endpoints allow manual module manipulation. Every one that changes the saved modules
   (merge, split, move, rename, delete, reference classes) re-runs the analyses as well, so a change made by hand refreshes the
   recommendations like an applied one (#28); the enforcement-mode switch does not, because no analysis reads the mode
6. **Rating Weights**: `GET`/`PUT /api/projects/{projectName}/rating-weights` read and replace the project's election and
   recommendation weights (#27). For the analysed project a `PUT` re-elects the saved modules, except reference classes
   chosen by hand, and re-runs the analyses; the page then reloads with `GET /api/module`, as after an applied
   recommendation
7. **Analysis Settings**: `GET`/`PUT /api/projects/{projectName}/analysis-settings` read and replace what the analyses
   recommend and show (#36). For the analysed project a `PUT` re-runs the analyses on the saved modules, without
   re-electing

### Key Metrics and Formulas

The architectural analysis framework relies on three core metrics:

| Metric                             | Formula                                                                                   | Range  | Purpose                                                        |
|------------------------------------|-------------------------------------------------------------------------------------------|--------|----------------------------------------------------------------|
| **Similarity**                     | `0.5 × [a/(a+b) + a/(a+c)]` where a=shared deps, b=unique to class 1, c=unique to class 2 | [0, 1] | Measures structural alignment between classes                  |
| **Avg Similarity with RefClazzes** | `(1/\|R\|) × Σ sim(c, rᵢ)` for reference classes R                                        | [0, 1] | Evaluates how well a class fits module's architectural pattern |
| **Violations**                     | `\|D(m) \ R(m)\|` where D(m)=module deps, R(m)=ref class deps                             | [0, ∞) | Counts architectural rule violations                           |

**Rating Mechanism:**

- Used by Split, Merge, and Move analyses to evaluate recommendations
- `rate = ω_sim × Δ_sim + ω_vio × Δ_vio_norm`, with the project's recommendation weights (0.5 / 0.5 by default)
- Only positive rates are recommended (net architectural benefit)
- Weights set per project by the architect; see Rating Weights

### DTO Structure

**Project DTOs** (`dto/projects/`)

- `ProjectAnalysesDTO` - Main analysis response: `id`, `projectName`, `modulesList`, `projectSimilarity`, `architecturalConstraints` (
  List<ArchitecturalConstraintDTO>)
- `ArchitecturalConstraintDTO` - Per-module constraints: `moduleId`, `moduleName`, `refClazzesDependencies`
- `ProjectDetailDTO` - Project summary: `name`
- `RatingWeightsDTO` - `election`, `recommendation`, each `{similarity, violation}`; validated by `toRatingWeights()`
- `ProjectsListResponseDTO` - List of projects

**Module DTOs** (`dto/modules/`)

- `ModuleDTO` (Serializable record) - Complete module: `id`, `name`, `refClazzes`, `refClazzesDependencies`, `allDependencies`, `clazzes`,
  `similarity`, `avgRefClazzesSimilarity`, `refClazzesChosenByHand`
- `SplitModuleRequest` - `moduleId`, `classIds`
- `SetRefClazzesRequest` - `moduleId`, `classIds`
- `SplitModuleResponse` - `splitModules`

**Class DTOs** (`dto/clazzes/`)

- `ClazzResponseDTO` (Serializable record) - `id`, `name`, `dependencies`, `similarity`, `avgSimilarityWithRefClazzes`, `firstModule`,
  `currentModule`

**Dependency DTOs** (`dto/dependencies/`)

- `DependencyDTO` (Serializable record) - `packageName`, `types` (List<TypeDTO>)
- `DependencyDTO.TypeDTO` - nested record: `fullyQualifiedName`, `className`

**Recommendation DTOs** (`dto/recommendations/`)

- `RecommendationsResponseDTO` - `splits`, `merges`, `moves`, `violations`, `summary`
- `SplitModuleRecommendationDTO` - `originalModuleId`, `originalModuleName`, `superRefClassName`, `deRefClassName`, `module1ClassIds`,
  `module2ClassIds`, `metrics`
- `MergeModuleRecommendationDTO` - `module1Id`, `module1Name`, `module2Id`, `module2Name`, `metrics`
- `MoveClassRecommendationDTO` - `classId`, `className`, `sourceModuleId`, `sourceModuleName`, `targetModuleId`, `targetModuleName`,
  `metrics`
- `ArchitectureViolationRecommendationDTO` - `classId`, `className`, `sourceModuleId`, `sourceModuleName`, `violation`,
  `violatingDependencies`, `bestSuggestion`, `alternativeSuggestions`
- `MoveSuggestionDTO` - `targetModuleId`, `targetModuleName`, `similarityImprovement`, `newViolationsCreated`, `requiresNewModule`,
  `suggestionReason`
- `SplitMetricsDTO`, `MergeMetricsDTO`, `MoveMetricsDTO` - Detailed metrics for each operation type
- `RecommendationsSummaryDTO` - `totalRecommendations`, `highPriority`, `mediumPriority`, `lowPriority`
- Every number the API writes has at most four decimal places, rounded half up (#71): `util/FourDecimalPlaces`,
  registered on Spring Boot's `ObjectMapper` by `config/JsonNumberPrecisionConfig`. The analyses, filters, `modules.bin` and
  the per-project JSON files keep full precision. The summary's priorities (`RecommendationPriority`) classify the rounded
  rate, as the web's badge does; the panel sorts by it, so rates equal to four places keep the API's order

### Key Configuration

- **File Upload**: Configured for large projects (max 200MB per file, 500MB request)
- **Projects Directory**: `uploads/` (configurable via `archanchor.projects-dir`; owned by `ProjectsDirectory`)
- **Repository Import**: JGit shallow clone of `github.com` only; per-operation timeout `archanchor.import.timeout-seconds`
- **Java Parser**: Latest versions (3.27.0) for Java 17+ support and binding resolution
- **Maven Integration**: Uses Maven Resolver API for dependency resolution

### Testing

- Uses Spring Boot Test framework
- Test files in `src/test/java/com/archanchor/`
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
- **Rating**: All structural recommendations use a rate combining similarity improvements and violation reductions, weighted per project
- **Persistence**: Modules are saved/loaded as serialized binary files via `ModuleService.saveModules()` / `getModulesFromFile()`;
  rating weights as JSON in `rating-weights.json`

### JavaParserService Implementation Constraints

**CRITICAL**: When working on JavaParserService or any parser implementation, DO NOT use hardcoded solutions for specific libraries or
dependencies. The parser must work generically with any Java project and any external libraries, not just the ones we're testing with. The
goal is to create a robust, generic solution that works with Spring Boot, plain Maven projects, and any other Java frameworks without
requiring specific library knowledge.
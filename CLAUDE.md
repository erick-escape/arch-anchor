# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Code style

- Functions: 4-20 lines. Split if longer.
- Files: under 500 lines. Split by responsibility.
- One thing per function, one responsibility per module (SRP).
- Names: specific and unique. Avoid `data`, `handler`, `Manager`.
  Prefer names that return <5 grep hits in the codebase.
- Types: explicit. No `any`, no `Dict`, no untyped functions.
- No code duplication. Extract shared logic into a function/module.
- Early returns over nested ifs. Max 2 levels of indentation.
- Exception messages must include the offending value and expected shape.

## Comments

- Keep your own comments. Don't strip them on refactor — they carry
  intent and provenance.
- Write WHY, not WHAT. Skip `// increment counter` above `i++`.
- Docstrings on public functions: intent + one usage example.
- Reference issue numbers / commit SHAs when a line exists because
  of a specific bug or upstream constraint.

## Tests

- Backend tests run with a single command: `./mvnw test`.
- Frontend tests run with `npm test` (vitest) from `web/`.
- Every new function gets a test. Bug fixes get a regression test.
- Mock external I/O (API, DB, filesystem) with named fake classes,
  not inline stubs.
- Tests must be F.I.R.S.T: fast, independent, repeatable,
  self-validating, timely.

## Dependencies

- Inject dependencies through constructor/parameter, not global/import.
- Wrap third-party libs behind a thin interface owned by this project.

## Structure

- Follow the framework's convention (Spring, React, etc.).
- Prefer small focused modules over god files.
- Predictable paths: controller/model/view, src/lib/test, etc.

## Formatting

- Use the language default formatter (`spring java format`, `prettier`). Don't discuss style beyond that.

## Logging

- Structured JSON when logging for debugging / observability.
- Plain text only for user-facing CLI output.

## Git workflow

- Every change starts from a GitHub issue. If none exists, open one first (`gh issue create`).
- Branch from an up-to-date `main`: `<type>/<issue>-<slug>`, e.g. `fix/1-typed-state`.
- Implement test-first (TDD): failing test, make it pass, then refactor. For type-only fixes
  the failing check is `npm run build` / `tsc -b` in `web/`.
- Open a PR into `main` using `.github/PULL_REQUEST_TEMPLATE.md` and link the issue:
  `Closes #N`, or `Refs #N` when the PR is one of several passes on the same issue.
- Merge once CI is green, then delete the branch.
- Commit messages are always in English: `type: subject` (`feat`, `fix`, `refactor`, `docs`,
  `test`, `ci`, `chore`), with a body that explains why.

## Project Overview

Arch Anchor is a full-stack tool that extracts architectural constraints from Java source code, centered on the reference class concept: the class whose dependency set best represents a module's architectural intent. The system parses Java codebases, computes similarity and violation metrics, recommends structural refactorings, and provides interactive visualization of modules, classes, and dependencies.

**Backend Stack:**
- Spring Boot 3.3.5 (Java 17) with JavaParser for code analysis
- JavaParser 3.27.0 (parsing + symbol resolution)
- MapStruct 1.5.5 (DTO mapping)
- Lombok (boilerplate reduction)
- Maven Resolver API (dependency resolution)

**Frontend Stack:**
- React 18 + TypeScript + Vite with ReactFlow for graph visualization
- ReactFlow (@xyflow/react) for graph visualization
- TanStack Query for server state management
- React Router DOM for navigation
- Axios for HTTP requests
- Ant Design for UI components

## Repository Structure

```
arch-anchor/
├── api/       # Spring Boot backend (Java 17, Maven wrapper bundled)
├── web/       # React frontend (Vite dev server, proxies /api to :8080)
└── examples/  # Sample Java project used to exercise the analyses
```

---

## Backend (`api/`)

### Build & Development Commands

```bash
cd api

# Development
./mvnw spring-boot:run              # Start backend server (port 8080)
./mvnw clean compile                # Clean and compile
./mvnw test                         # Run all tests
./mvnw test -Dtest=ClassName        # Run specific test class
./mvnw clean package                # Build JAR
```

### Architecture Overview

**Controllers** (`src/main/java/com/archanchor/controllers/`)
- REST endpoints for project upload, analysis, and module/class operations
- Key endpoints: `/api/upload`, `/api/analyze`, `/api/projects`, `/api/module/*`, `/api/recommendations/*`

**Services** (`src/main/java/com/archanchor/services/`)
- `JavaParserService` - Core parsing and dependency resolution using JavaParser
- `ProjectService` - Orchestrates project analysis workflow
- `ModuleService` - Module management, similarity calculations, violation tracking, module manipulation
- `RecommendationsService` - Aggregates results from all analyses into recommendation DTOs
- `ClazzService`, `DependencyService` - Placeholders (currently empty)

**Architectural Analyses Framework** (`src/main/java/com/archanchor/services/architecturalAnalyses/`)
- Extensible framework for analyzing module structure and generating recommendations
- `ArchitecturalAnalysesRunner` - Discovers and executes all analyses post-parsing
- Current analyses: `SplitModule`, `MergeModule`, `MoveClass`, `ArchitectureViolation`
- All structural analyses use balanced rating: `rate = 0.5 × Δ_sim + 0.5 × Δ_vio_norm`
- Only recommendations with positive rate (net architectural benefit) are suggested
- Results exposed via `RecommendationsService` and `RecommendationsController`

**Domain Models** (`src/main/java/com/archanchor/domains/`)
- `Module` - Logical module with: `refClazzes`, `refClazzesDependencies`, `moduleDependencies`, `clazzes`, `similarity`, `avgRefClazzesSimilarity`, `violations`
- `Clazz` - Class with: `dependencies`, `similarity`, `avgSimilarityWithRefClazzes`, `firstModule`, `currentModule`
- `Dependency` - Dependency on an external package: `packageName`, `types` (List<Type>)
- `Type` - Specific type within a dependency: `fullyQualifiedName`, `className`

### Key Concepts

**Reference Classes:**
- Every module MUST have at least one reference class defining architectural intent
- Selected automatically via rating (similarity + violations) or manually by architect
- Reference class dependencies stored in `module.getRefClazzesDependencies()` define architectural constraints

**Metrics:**
- **Similarity**: `0.5 × [a/(a+b) + a/(a+c)]` where a=shared deps, b/c=unique deps (range [0,1])
- **Violations**: Count of `moduleDependencies` NOT present in `refClazzesDependencies`
- **Avg Similarity with RefClazzes**: Evaluates how well a class fits module's pattern

**Rating Mechanism:**
- `rate = 0.5 × Δ_sim + 0.5 × Δ_vio_norm`
- Used by Split, Merge, and Move analyses
- Weights configurable via `ModuleService.SIMILARITY_WEIGHT` and `VIOLATION_WEIGHT`

### Important Implementation Constraints

**CRITICAL**: When working on `JavaParserService` or any parser implementation, DO NOT use hardcoded solutions for specific libraries or dependencies. The parser must work generically with any Java project and any external libraries. The goal is a robust, generic solution for Spring Boot, plain Maven projects, and any Java frameworks.

### Testing

Test files in `src/test/java/com/archanchor/`:
- `JavaParserTypeResolutionTest` - JavaParser functionality
- `SplitModuleTest`, `MergeModuleTest`, `MoveClassTest` - Analysis logic
- `ArchitectureViolationTest` - Violation detection

---

## Frontend (`web/`)

### Build & Development Commands

```bash
cd web

# Development
npm install                         # Install dependencies
npm run dev                         # Start dev server (port 5173)
npm run build                       # Build for production
npm run lint                        # Run ESLint
npm run preview                     # Preview production build
```

### Architecture Overview

**Routing** (`src/App.tsx`)
- `/` - Projects list page
- `/analyze/:projectName` - Interactive analysis visualization

**Screens** (`src/screens/`)
- `Projects/` - Project management (upload, list, delete, analyze)
- `Analyze/` - ReactFlow-based graph visualization with drag-and-drop module merging

**Components** (`src/components/`)
- `Header/` - Application header with navigation
- `Sidebar/` - Module/class details and operations panel
- `UploadModal/` - File upload for new projects
- `Popup/` - Merge confirmation dialog

**Services & Hooks**
- `services/api.tsx` - Axios instance (baseURL: localhost:8080)
- `hooks/apiHooks.tsx` - TanStack Query mutations for file uploads

**Data Models** (`src/interface/`)
- `ModuleData` - Module with id, name, refClass, clazzes[], dependencies[], similarity
- `ClazzData` - Class with id, name, dependencies[], similarity, firstModule, currentModule
- `Dependency` - Dependency relationships
- `MethodData`, `AttributeData` - Class members

### Key Features

**Interactive Visualization:**
- ReactFlow graph with custom nodes for modules
- Concentric circle layout (first module centered, others in rings)
- Drag-and-drop nodes onto each other to trigger merge confirmation
- Real-time updates after operations (split, merge, move)

**API Integration:**
- Proxied through Vite dev server: `/api` → `http://localhost:8080`
- Automatic analysis trigger on project page load
- Polling-based or event-based updates for module changes

**UI Framework:**
- Ant Design (antd) for forms, tables, buttons, modals
- FontAwesome icons
- Custom styling with TypeScript style objects

---

## Full-Stack Development Workflow

1. **Start Backend**: `cd api && ./mvnw spring-boot:run`
2. **Start Frontend**: `cd web && npm run dev`
3. **Access Application**: http://localhost:5173
4. **Upload Project**: Click "Add Project" and upload Java source files - `examples/sample-architecture-project` is the fixture that exercises all three analyses
5. **Analyze**: Click "Analyze" on a project - backend parses, runs analyses, frontend visualizes

## API Proxy Configuration

Frontend Vite dev server proxies `/api` requests to backend (vite.config.ts):
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```
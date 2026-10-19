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
└── examples/  # The bookstore: the sample Java project that exercises the analyses
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
- REST endpoints for project upload or GitHub import, analysis, and module/class operations
- Key endpoints: `/api/upload`, `/api/projects/import`, `/api/analyze`, `/api/projects`, `/api/projects/{projectName}/rating-weights`, `/api/projects/{projectName}/analysis-settings`, `/api/module/*`, `/api/recommendations/*`

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
- All structural analyses use the project's recommendation weights: `rate = w_sim × Δ_sim + w_vio × Δ_vio_norm` (0.5 each by default), through `ArchitecturalAnalysis.recommendationRate`
- Only recommendations with positive rate (net architectural benefit) are suggested
- Results exposed via `RecommendationsService` and `RecommendationsController`

**Domain Models** (`src/main/java/com/archanchor/domains/`)
- `Module` - Logical module with: `refClazzes`, `refClazzesDependencies`, `moduleDependencies`, `clazzes`, `similarity`, `avgRefClazzesSimilarity`, `violations`, `refClazzesChosenByHand`
- `Clazz` - Class with: `dependencies`, `similarity`, `avgSimilarityWithRefClazzes`, `firstModule`, `currentModule`
- `Dependency` - Dependency on an external package: `packageName`, `types` (List<Type>)
- `Type` - Specific type within a dependency: `fullyQualifiedName`, `className`

### Key Concepts

**Reference Classes:**
- Every module MUST have at least one reference class defining architectural intent
- Selected automatically via rating (similarity + violations, weighted by the project's election weights) or manually by architect; a module whose reference classes were chosen by hand (`refClazzesChosenByHand`) keeps them when the election weights change
- A merged module keeps the reference classes of both modules, as its merge card was rated (#34), and counts as chosen by hand
- Reference class dependencies stored in `module.getRefClazzesDependencies()` define architectural constraints

**Metrics:**
- **Similarity**: `0.5 × [a/(a+b) + a/(a+c)]` where a=shared deps, b/c=unique deps (range [0,1])
- **Violations**: Count of `moduleDependencies` NOT present in `refClazzesDependencies`
- **Module scope** (#43): within a module, a reference to one of its own classes (or a type nested in one) is internal and counts for neither similarity, dependencies, violations nor the election (`ModuleScope`). The parser already drops same-package references; this keeps that true after a simulated or applied move or merge. Each `Clazz` records its declared `packageName` for this
- **Avg Similarity with RefClazzes**: Evaluates how well a class fits module's pattern

**Rating Mechanism:**
- Election: `w_sim × sim_norm + w_vio × vio_norm` per class; recommendation rate: `w_sim × Δ_sim + w_vio × Δ_vio_norm`, used by Split, Merge, and Move
- Two weight pairs per project (`RatingWeights`: `election`, `recommendation`), each a `WeightPair` that is non-negative and sums to 1, default 0.5 / 0.5
- Set by the architect from the analysis page (**Settings**) or `PUT /api/projects/{projectName}/rating-weights`, stored per project in `rating-weights.json` (`JsonFileRatingWeightsStore`)
- `ModuleService` and the analyses read the weights in force from the injected `ActiveRatingWeights`, which `ProjectService.analyzeProject` sets to the analysed project's
- `examples/bookstore` at the defaults is pinned by `BookstoreRunningExampleTest`: its numbers are quoted in the README and the ICSE 2027 paper

**Analysis Settings** (#36):
- Per project (`AnalysisSettings`), set from **Settings** or `PUT /api/projects/{projectName}/analysis-settings`, stored in `analysis-settings.json`; the defaults filter nothing
- Minimum classes per split half, minimum classes a move leaves in its source, maximum move targets per class, minimum package dependencies of a class before it gets move or violation entries, minimum rate of a shown split, merge or move (violations are always shown), lowest similarity change of a recommended merge (#41)
- Read through `ActiveAnalysisSettings`; changing them re-runs the analyses without re-electing

**Import Source** (#66):
- A project imported from GitHub keeps its `ImportSource` (repository URL, subdirectory, ref, commit SHA) in `import-source.json` through `ImportSourceStore`; written by `RepositoryImportService`, forgotten on delete
- `GET /api/projects` items carry it as `source`, null for an uploaded folder; the project cards show it

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

The UI follows the redesign of #58 (brief: `docs/design/redesign-brief.md`).

**Routing** (`src/AppRoutes.tsx`)
- `/` - Homepage: import bar (GitHub URL with Options, folder upload, page-wide folder drop, "Try it on the bookstore example"), claims carousel, how it works, your projects, research
- `/projects` - Project cards, each with the repository it came from (#66)
- `/analyze/:projectName` - The analysis, outside the shell so the graph gets the whole screen
- `*` - Not found
- `CommandPaletteProvider` (`src/App.tsx`) gives every page the ⌘K / Ctrl K palette

**Theme** (`src/theme/`)
- `palette.ts` is the one source of every colour: `cssVariables.ts` turns it into `--aa-*` CSS variables, `antdTheme.ts` into the antd theme; `contrast.test.ts` checks WCAG contrast in both themes
- System / Light / Dark through `useThemePreference` (`ThemePreferenceStore`, `ColorSchemeQuery`); `index.html` sets `data-theme` before first paint
- Styles are CSS modules reading `--aa-*` variables; no literal colours in components

**Screens** (`src/screens/`)
- `Home/` - `importBar/` (`useProjectImport`, `droppedFolder.ts`, `usePageFolderDrop`), `claims/` (`useCarousel`, vignettes of the bookstore)
- `Projects/` - cards, delete behind a named confirmation
- `Analyze/` - `graph/` (module→module edges from `clazz.packageName`, layered layout with dagre, nodes and edges), `modules/` (list, detail, class selection toolbar, module menu), `recommendations/` (`recommendationCards.ts` keeps the Refactorings order and `CardList` the 50-card pages that `ICSE/evaluation/harness/panel.py` copies), `graphEmphasis` (hover highlight, applied-change motion), `usePanelLayout` (foldable panels, drawers below 1024 px), `useAnalyzeCommands`

**Components** (`src/components/`)
- `vocabulary/` - Logo, ClassTile, ModuleContainer, PackageChip, Meter, StatTile, PriorityBadge, DependencyArrow, ModeToggle, ThemeSwitch
- `AppShell/`, `AppHeader/`, `CommandPalette/`, `ConfirmDialog/`, `feedback/useToast`, `SettingsModal/` (Rating weights, Analysis filters)

**Services & Hooks**
- `services/*Api.ts` - one module per API resource, relative `/api` paths through the Vite proxy; failures become sentences (`problemDetail.ts`)
- `hooks/useRecommendations.ts` - keyed by project, enabled once that project's analysis returns (#53)

**Data Models** (`src/interface/`)
- `ModuleData`, `ClazzData` (with `packageName`), `Dependency`, `Project` (`ProjectSummary` with its `ImportSource`), `Recommendations`, `RatingWeights`, `AnalysisSettings`

**Tests**
- vitest + jsdom + Testing Library; `src/test/renderWithProviders.tsx` mounts screens like `main.tsx`, `src/test/fakes/` holds the named fakes (`FakeAxiosAdapter`, theme fakes), `src/test/fixtures/` the bookstore and recommendation fixtures

---

## Full-Stack Development Workflow

1. **Start Backend**: `cd api && ./mvnw spring-boot:run`
2. **Start Frontend**: `cd web && npm run dev`
3. **Access Application**: http://localhost:5173
4. **Add Project**: On the homepage, paste a GitHub repository or upload a folder - `examples/bookstore` is the fixture: one misplaced class (`web.SalesReport`, which uses `java.sql`) triggers a violation and all three analyses, and its README records the tool's actual output (**Try it on the bookstore example** fills in `https://github.com/erick-escape/arch-anchor` with subdirectory `examples/bookstore` and project name `bookstore`)
5. **Analyze**: The analysis opens after the import, or from **Analyze** on a project card - backend parses, runs analyses, frontend visualizes

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
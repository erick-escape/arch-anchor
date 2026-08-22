# Arch Anchor

**Recover the architecture your code already has.**

Arch Anchor reads a Java codebase and extracts its *architectural constraints* — the rules
about which module may depend on which package — without requiring any architectural
documentation up front. It does this by identifying a **reference class** per module: the class
whose dependency set best represents what that module was meant to be. The reference class's
dependencies become the module's contract, and everything that steps outside it is reported as
a violation.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)
![Node 18+](https://img.shields.io/badge/Node-18%2B-green)

---

## What it does

Point it at a Java project and it will:

1. **Parse** every source file with JavaParser, resolving imports to fully-qualified types.
2. **Group** classes into modules by package structure.
3. **Elect a reference class** per module automatically, by rating each candidate on how well its
   dependency set represents the module and how few violations it implies. You can override the
   choice — or pick several reference classes — from the sidebar.
4. **Measure** each module: similarity between its classes, and violations (dependencies the
   module uses that the reference class does not sanction).
5. **Recommend** structural refactorings — split a module, merge two, move a class — but only
   where the change is a net architectural improvement.
6. **Export** the recovered constraints as a PDF:

```
AC1 - database CAN-DEPEND java.sql
AC2 - database CAN-DEPEND javax.persistence
AC3 - service   MUST-DEPEND com.example.database
```

`CAN-DEPEND` is a permission: the module is allowed to reach that package. `MUST-DEPEND` is an
obligation: switch a reference class to `MUST` mode when the dependency is not merely tolerated
but required.

## Repository layout

```
arch-anchor/
├── api/       Spring Boot 3.3 backend — parsing, metrics, analyses (Java 17)
├── web/       React 18 + TypeScript + Vite frontend — graph UI, AC export
└── examples/  A sample Java project to analyze on your first run
```

Both halves live in one repository on purpose. They are developed as a single tool, released
together, and a change to an endpoint almost always changes its caller — so they belong in one
commit. Each side still keeps its own toolchain untouched: `api/` builds with the bundled Maven
wrapper, `web/` with npm, and neither drives the other.

## Prerequisites

- **Java 17** or newer
- **Node.js 18** or newer, with npm
- No global Maven install — `api/` ships the Maven wrapper (`./mvnw`)

## Quick start

Two terminals. First the backend:

```bash
cd api
./mvnw spring-boot:run
```

It starts on **http://localhost:8080**, ending with `Started ArchAnchorApplication in N seconds`.

Then the frontend:

```bash
cd web
npm install
npm run dev
```

Open **http://localhost:5173**. If the project list renders, both halves are talking to each
other. (The frontend reaches the backend through the Vite dev-server proxy — see
[Limitations](#limitations).)

## Your first analysis

Use the bundled example to see what the tool reports before you point it at your own code:

1. Click **Add Project**, select the directory `examples/sample-architecture-project`, and upload.
2. Click **Analyze**. The graph fills with modules and each one gets a reference class.
3. Open the recommendations panel. This project is built to trigger one of each analysis:

   | Recommendation | Why |
   |---|---|
   | Split `service` | It holds two unrelated dependency domains — request handling and batch processing |
   | Merge `auth` + `security` | Both revolve around the same four dependencies |
   | Move `database.UIRenderer` → `presentation` | Its dependencies match presentation, not database |

The tool reports other, lower-rated suggestions alongside these three;
`examples/sample-architecture-project/README.md` describes each scenario in full.

## Running it on your own project

1. **Upload it.** Click **Add Project** and select your project's root directory (or just its
   `src/main/java`). The browser uploads the whole directory tree; only `.java` files are parsed,
   so extra resources and configuration files are harmless. It does not need to compile, and
   dependencies do not need to be installed.
2. **Analyze it.** Modules are derived from your package structure — one module per package that
   holds classes. Expect the module names to mirror your package names.
3. **Check the reference classes.** Open the sidebar and review the class the tool elected for
   each module. This is the one judgement call worth your attention: the reference class defines
   what the module is *allowed* to depend on, so every constraint and every violation downstream
   follows from it. Override it, or select several, when you disagree.
4. **Set enforcement per reference class.** `ALLOW` (the default) exports as `CAN-DEPEND`; switch
   to `MUST` for dependencies the module is required to have, which exports as `MUST-DEPEND`.
5. **Reshape modules if you want.** Rename, delete, split, merge (drag one module node onto
   another), or move individual classes between modules. Metrics recompute after each operation.
6. **Export.** The three-dot menu in the top-right corner → **Export ACs** downloads a PDF of
   every constraint the tool recovered.

## Concepts

| Concept | Definition |
|---|---|
| **Reference class** | The class whose dependency set best represents a module's architectural intent. Every module has at least one; its dependencies are the module's contract. |
| **Similarity** | `0.5 × [a/(a+b) + a/(a+c)]`, where `a` is dependencies two classes share and `b`, `c` are those unique to each. Range `[0,1]`. |
| **Violation** | A dependency a module uses that its reference classes do not sanction. |
| **Rate** | `0.5 × Δ_similarity + 0.5 × Δ_violations_normalized`. Split, merge, and move are each scored this way, and only positive-rate changes are ever suggested — a refactoring has to improve cohesion *and* not cost more violations than it saves. |

Weights are configurable via `SIMILARITY_WEIGHT` and `VIOLATION_WEIGHT` in
`api/src/main/java/com/archanchor/services/ModuleService.java`.

## API reference

All endpoints are under `http://localhost:8080`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/projects` | List uploaded projects |
| `POST` | `/api/upload` | Upload a project (multipart `files`) |
| `DELETE` | `/api/projects/{projectName}` | Delete an uploaded project |
| `POST` | `/api/analyze?projectName=` | Run the full pipeline: parse, group, elect reference classes, measure, analyze |
| `POST` | `/api/module/merge?sourceId=&targetId=` | Merge two modules |
| `POST` | `/api/module/split` | Split a module (JSON body) |
| `POST` | `/api/module/rename?moduleId=&newName=` | Rename a module |
| `DELETE` | `/api/module/delete?moduleId=` | Delete a module |
| `POST` | `/api/module/ref-clazzes` | Set a module's reference classes (JSON body) |
| `POST` | `/api/module/ref-clazz-mode` | Set a reference class's enforcement mode: `ALLOW` or `MUST` |
| `POST` | `/api/module/move-class?classId=&sourceModuleId=&targetModuleId=` | Move a class between modules |
| `GET` | `/api/recommendations` | All recommendations from the last analysis |
| `POST` | `/api/recommendations/apply/split` | Apply a split recommendation |
| `POST` | `/api/recommendations/apply/merge?sourceId=&targetId=` | Apply a merge recommendation |
| `POST` | `/api/recommendations/apply/move?classId=&sourceModuleId=&targetModuleId=` | Apply a move recommendation |

## Development

**Backend** (`api/`)

| Command | Description |
|---|---|
| `./mvnw spring-boot:run` | Start the API on port 8080 |
| `./mvnw test` | Run the test suite |
| `./mvnw test -Dtest=SplitModuleTest` | Run one test class |
| `./mvnw clean compile` | Compile |
| `./mvnw clean package` | Build the executable JAR |

**Frontend** (`web/`)

| Command | Description |
|---|---|
| `npm run dev` | Dev server on port 5173, proxying `/api` to 8080 |
| `npm test` | Run the vitest suite |
| `npm run lint` | Run ESLint |
| `npm run format` | Format with Prettier |
| `npm run build` | Type-check and build for production — see [Limitations](#limitations) |

**Local state.** The backend keeps everything on disk next to itself, all gitignored:

| Path | Contents |
|---|---|
| `api/uploads/` | Uploaded project sources, one directory per project |
| `api/modules.bin` | The current module graph |
| `api/project-analyses.bin` | Cached analysis results |

Delete all three to reset the tool to a blank slate. The `.bin` files are Java-serialized, so
they are also tied to the current class names — a stale file from an older version of the code
will fail to load, and deleting it is the fix.

## Limitations

- **Development setup only.** Most of the frontend calls the API through relative `/api` paths,
  which depend on the Vite dev-server proxy in `web/vite.config.ts`. A static `npm run build`
  deployment needs a reverse proxy in front of both halves; there is no production configuration
  in the repo yet.
- **`npm run build` currently fails.** `tsc -b` reports pre-existing type errors in the UI
  components — untyped props and `useState([])` widening to `never[]`. This does not affect
  `npm run dev`, which is how the tool is meant to be run today, and it is tracked in
  [issue #1](https://github.com/erick-escape/arch-anchor/issues/1). `npm run lint` and
  `npm test` do pass.
- **No database.** State lives in memory and in the two `.bin` files described above. Restarting
  the backend without those files means re-uploading.
- **Java only**, and modules are inferred from package structure — the tool has no notion of
  Maven modules, Gradle subprojects, or JPMS.
- `JavaParserTypeResolutionTest` is skipped unless `api/uploads/pass-in` is present. It asserts
  against a specific local project rather than checked-in fixtures.

## Citation and research artifact

Arch Anchor is the tool behind:

> **Towards the Extraction of Architectural Constraints centered on the Reference Class Concept**
> Érick de Castro Silva (UFLA), Eduardo Guerra (Free University of Bozen-Bolzano),
> Ricardo Terra (UFLA). ECSA 2026.

Applied to LEONA, a real-world Java system, the tool automatically recovered 21 of 39 manually
defined architectural constraints and surfaced 20 more that had never been documented.

The archived artifact — including the LEONA sources, the 32 constraints the tool produced from
them, and step-by-step reproduction instructions — is on Zenodo:
**https://zenodo.org/records/20057336**

That replication package is deliberately *not* bundled here. This repository is the maintained
tool; the Zenodo record is the frozen snapshot the paper's results came from. Constraints
generated by the tool are the user's own; the archived constraints PDF is CC-BY 4.0.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and pull requests are welcome.

## License

MIT — see [LICENSE](LICENSE).

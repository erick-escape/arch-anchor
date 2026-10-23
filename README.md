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
   dependency set represents the module and how few violations it implies, weighted as you
   choose. You can override the choice — or pick several reference classes — from the modules
   panel.
4. **Measure** each module: similarity between its classes, and violations (dependencies the
   module uses that the reference class does not sanction).
5. **Recommend** structural refactorings — split a module, merge two, move a class — but only
   where the change is a net architectural improvement.
6. **Export** the recovered constraints as a PDF:

```
AC4 - bookstore/web CAN-DEPEND bookstore.model
AC5 - bookstore/web CAN-DEPEND bookstore.service
AC6 - bookstore/persistence MUST-DEPEND bookstore.model
AC7 - bookstore/persistence MUST-DEPEND java.sql
```

(The last four of the seven constraints the tool exports for the bundled example.)

`CAN-DEPEND` is a permission: the module is allowed to reach that package. `MUST-DEPEND` is an
obligation: switch a reference class to `MUST` mode when the dependency is not merely tolerated
but required.

## Repository layout

```
arch-anchor/
├── api/       Spring Boot 3.3 backend — parsing, metrics, analyses (Java 17)
├── web/       React 18 + TypeScript + Vite frontend — graph UI, AC export
└── examples/  The bookstore, a small Java project to analyze on your first run
```

Both halves live in one repository on purpose. They are developed as a single tool, released
together, and a change to an endpoint almost always changes its caller — so they belong in one
commit. Each side still keeps its own toolchain untouched: `api/` builds with the bundled Maven
wrapper, `web/` with npm, and neither drives the other. The `Dockerfile` at the root builds both
and packages the frontend into the backend's jar, which is the form the tool is released in.

## Run it

There is nothing to build. Either way below serves the whole tool on one port.

**With Docker**

```bash
docker run --rm -p 8080:8080 ghcr.io/erick-escape/arch-anchor:1.0.0
```

Open **http://localhost:8080** once the log ends with `Started ArchAnchorApplication in N
seconds`. The image runs on x86-64 and on ARM, Apple silicon included.

- If port 8080 is taken, publish another one: `-p 8090:8080`, then open http://localhost:8090.
- The tool keeps its projects in the container's `/data`, so `--rm` discards them when the
  container stops. Add `-v archanchor-data:/data` to keep them between runs.

**With Java 17 or newer**

Download `arch-anchor-1.0.0.jar` from the
[v1.0.0 release](https://github.com/erick-escape/arch-anchor/releases/tag/v1.0.0) and run it:

```bash
java -jar arch-anchor-1.0.0.jar
```

It also serves **http://localhost:8080** (add `--server.port=8090` for another port), and keeps
its state in the directory you start it from (see [Local state](#development)).

Either way, the bookstore example below is the quickest check that the tool works. Its shortcut
imports the example from GitHub, so it needs an internet connection; offline, unpack the
release's source code and use **Upload folder** on `examples/bookstore`.

To work on the tool itself, see [Development](#development).

## Your first analysis

Use the bundled example to see what the tool reports before you point it at your own code. It
is a small bookstore in four layers, `web`, `service`, `persistence` and `model`, with one
mistake: `SalesReport` sits in the web layer and queries the database itself.

1. On the home page, click **Try it on the bookstore example**. It fills in
   `https://github.com/erick-escape/arch-anchor` with **Subdirectory** `examples/bookstore` and
   **Project name** `bookstore`. Click **Analyze**. (From a clone, **Upload folder** takes
   `examples/bookstore` instead, or drop the folder anywhere on the page.)
2. The analysis opens once the import is done. Four modules appear on the graph, each with its
   reference class. `bookstore/web` elects `CatalogController` and stands at 75.00 %
   similarity; the other three are at 100 %. A dashed red arrow runs to `java.sql`.
3. The recommendations panel is on the right. The controllers depend on the service layer and
   the model, so `java.sql` in `SalesReport` is a violation, and the tool ranks three ways to
   remove it:

   | Recommendation | Rate | Why |
   |---|---|---|
   | Split `bookstore/web` | 0.63 | `SalesReport` on its own, apart from the three controllers |
   | Move `SalesReport` → `bookstore/persistence` | 0.56 | Its dependencies are exactly a repository's |
   | Merge `bookstore/persistence` + `bookstore/web` | 0.42 | `persistence` may use `java.sql`; rated last because it costs cohesion |
   | Fix violation in `SalesReport` | | Names the forbidden package and suggests `bookstore/persistence` |

4. Click **Apply move…** on the move and confirm. `SalesReport` travels to
   `bookstore/persistence`; every module is now at 100 %, with no violations and nothing left
   to recommend.

[`examples/bookstore/README.md`](examples/bookstore/README.md) has the full output, including
how the reference class of `web` is elected and the constraints the tool exports.

## Running it on your own project

1. **Add it** from the home page's import bar. The project needs a `src/` directory directly
   under its root; it does not need to compile, and dependencies do not need to be installed.
   - *From GitHub:* paste the repository URL and click **Analyze**. The backend clones the latest
     commit of the default branch, or of the branch or tag you name under **Options**, and reports
     which commit it took. If the repository keeps its code deeper, for example one Maven module
     per directory, set **Subdirectory** to the module to analyze; when it is missing, the error
     lists the directories that would work. A private repository needs a personal access token
     that can read its contents. The token is used for that one clone and is never stored.
   - *From disk:* click **Upload folder** and select your project's root directory, or drop the
     folder anywhere on the home page. The browser uploads the whole tree; only `.java` files
     are parsed, so extra resources and configuration files are harmless.

   **Projects** lists everything imported, with the repository each came from.
2. **Analyze it.** The analysis opens after the import, and **Analyze** on a project card opens
   it again later. Modules are derived from your package structure — one module per package that
   holds classes. Expect the module names to mirror your package names.
3. **Check the reference classes.** Open each module in the modules panel on the left and review
   the class the tool elected for it. This is the one judgement call worth your attention: the reference class defines
   what the module is *allowed* to depend on, so every constraint and every violation downstream
   follows from it. Override it, or select several, when you disagree: tick the classes and
   choose **Set as reference**.
4. **Weigh similarity against violations, if you want.** **Settings** in the top bar sets
   two pairs of weights for the project, each on one slider: the **election**, which decides the
   reference classes, and the **recommendation rate**, which ranks the refactorings. Both start
   at 0.5 / 0.5. Saving re-elects every module except those whose reference classes you chose by
   hand, and re-rates the recommendations, without discarding your module operations. Its
   **Analysis filters** tab hides recommendations you do not want, such as splits that leave a
   one-class module, or merges, moves and splits that lower similarity.
5. **Set enforcement per reference class.** `ALLOW` (the default) exports as `CAN-DEPEND`; switch
   to `MUST` for dependencies the module is required to have, which exports as `MUST-DEPEND`.
6. **Reshape modules if you want.** A module's **⋮** menu renames, merges or deletes it;
   dragging one module onto another on the graph also merges them, and ticking classes in a
   module splits them off. Metrics recompute after each operation. **Apply…** on a card in the
   recommendations panel performs that split, merge or move for you once you confirm, and the
   panel then lists the recommendations for the new structure. None of this touches your source
   files: analyzing the project again starts over from them.
7. **Export.** **Export ACs** in the top bar previews every constraint the tool recovered,
   grouped by module, for the modules as they are on screen, then downloads them as a PDF.

**⌘K** (Ctrl K) opens a command palette on every page: jump to a module, a class or a
recommendation, open any project, or switch between the light and dark themes. Below 1024 px
the two panels of the analysis become drawers, opened from the top bar.

## Concepts

| Concept | Definition |
|---|---|
| **Reference class** | The class whose dependency set best represents a module's architectural intent. Every module has at least one; its dependencies are the module's contract. |
| **Similarity** | `0.5 × [a/(a+b) + a/(a+c)]`, where `a` is dependencies two classes share and `b`, `c` are those unique to each. Range `[0,1]`. |
| **Violation** | A dependency a module uses that its reference classes do not sanction. |
| **Election** | Each class of a module is rated `w_sim × similarity_normalized + w_viol × violations_normalized`, the violations being those the module would have with that class as its reference; the best-rated class is elected. |
| **Rate** | `w_sim × Δ_similarity + w_viol × Δ_violations_normalized`. Split, merge, and move are each scored this way, and only positive-rate changes are ever suggested — a refactoring has to improve cohesion *and* not cost more violations than it saves. |
| **Rating weights** | Two `(w_sim, w_viol)` pairs per project, one for the election and one for the rate, each non-negative and summing to 1. Both default to 0.5 / 0.5. Set them from **Settings** on the analysis page or through `/api/projects/{projectName}/rating-weights`. |

## API reference

All endpoints are under `http://localhost:8080`.
Every number in a response has at most four decimal places, rounded half up.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/projects` | List uploaded projects |
| `POST` | `/api/upload` | Upload a project (multipart `files`) |
| `POST` | `/api/projects/import` | Import a project from GitHub (JSON body: `repositoryUrl`; optional `ref`, `subdirectory`, `projectName`, `accessToken`) |
| `DELETE` | `/api/projects/{projectName}` | Delete an uploaded project |
| `GET` | `/api/projects/{projectName}/rating-weights` | The project's election and recommendation weights |
| `PUT` | `/api/projects/{projectName}/rating-weights` | Replace them (JSON body: `election` and `recommendation`, each `{similarity, violation}` summing to 1); for the analysed project, re-elects the saved modules, except reference classes chosen by hand, and re-rates the recommendations |
| `POST` | `/api/analyze?projectName=` | Run the full pipeline: parse, group, elect reference classes, measure, analyze |
| `GET` | `/api/module` | The modules as last saved, after every split, merge and move since the analysis |
| `POST` | `/api/module/merge?sourceId=&targetId=` | Merge two modules |
| `POST` | `/api/module/split` | Split a module (JSON body) |
| `POST` | `/api/module/rename?moduleId=&newName=` | Rename a module |
| `DELETE` | `/api/module/delete?moduleId=` | Delete a module |
| `POST` | `/api/module/ref-clazzes` | Set a module's reference classes (JSON body) |
| `POST` | `/api/module/ref-clazz-mode` | Set a reference class's enforcement mode: `ALLOW` or `MUST` |
| `POST` | `/api/module/move-class?classId=&sourceModuleId=&targetModuleId=` | Move a class between modules |
| `GET` | `/api/recommendations` | The recommendations for the current modules: computed by the last analysis, and again after each applied recommendation |
| `POST` | `/api/recommendations/apply/split` | Apply a split recommendation |
| `POST` | `/api/recommendations/apply/merge?sourceId=&targetId=` | Apply a merge recommendation |
| `POST` | `/api/recommendations/apply/move?classId=&sourceModuleId=&targetModuleId=` | Apply a move recommendation |

## Development

**Running from source.** You need **Java 17** or newer and **Node.js 18** or newer, with npm.
Maven is not required: `api/` ships the Maven wrapper (`./mvnw`). Two terminals. First the
backend:

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

Open **http://localhost:5173**. If the home page lists your projects under **Your projects**
(or shows only the import bar, before your first import), both halves are talking to each
other. The frontend calls the API through relative `/api` paths, which the Vite dev server
proxies to port 8080 (`web/vite.config.ts`); the packaged tool serves both on one port instead.

**Backend** (`api/`)

| Command | Description |
|---|---|
| `./mvnw spring-boot:run` | Start the API on port 8080 |
| `./mvnw test` | Run the test suite |
| `./mvnw test -Dtest=SplitModuleTest` | Run one test class |
| `./mvnw clean compile` | Compile |
| `./mvnw clean package` | Build the executable JAR, without the frontend |

**Frontend** (`web/`)

| Command | Description |
|---|---|
| `npm run dev` | Dev server on port 5173, proxying `/api` to 8080 |
| `npm test` | Run the vitest suite |
| `npm run lint` | Run ESLint |
| `npm run format` | Format with Prettier |
| `npm run build` | Type-check and build for production |

**Packaging** (repository root; needs Docker with BuildKit, i.e. the buildx plugin)

| Command | Description |
|---|---|
| `docker build -t arch-anchor .` | Build the image: the frontend, packaged into the backend's jar, on a Java 17 runtime |
| `docker build --target jar --output type=local,dest=out .` | Build only that jar, into `out/arch-anchor.jar` |

Publishing a GitHub release runs `.github/workflows/release.yml`, which pushes the image to
`ghcr.io/erick-escape/arch-anchor` for amd64 and arm64, tagged with the release's version and
`latest`, and attaches the jar to the release.

**Local state.** The backend keeps everything on disk in its working directory: `api/` under
`./mvnw spring-boot:run`, `/data` in the container, and wherever you start the jar. Under `api/`
it is all gitignored:

| Path | Contents |
|---|---|
| `api/uploads/` | Uploaded or imported project sources, one directory per project; imports in progress are staged in `.staging/` |
| `api/modules.bin` | The current module graph |
| `api/project-analyses.bin` | Cached analysis results |
| `api/rating-weights.json` | Each project's rating weights; a project without an entry has equal weights |
| `api/analysis-settings.json` | Each project's analysis filters; a project without an entry filters nothing |
| `api/import-source.json` | The repository, subdirectory, ref and commit each imported project came from |

Delete them all to reset the tool to a blank slate. The `.bin` files are Java-serialized, so
they are also tied to the current class names — a stale file from an older version of the code
will fail to load, and deleting it is the fix.

## Limitations

- **One analysed project, one user, per instance.** The backend holds the modules of the project
  analysed last (`modules.bin`), so analysing another project replaces them, and two people
  sharing one instance would overwrite each other's work. Run an instance per person; the
  Docker image makes that cheap.
- **No database.** State lives in memory and in the two `.bin` files described above. Restarting
  the backend without those files means adding the project again.
- **GitHub only for imports.** The importer clones from `github.com` and nowhere else; code on
  other hosts goes through the directory upload. An import runs inside the request, so a very
  large repository keeps it open until the clone finishes.
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

To cite the tool itself, use [`CITATION.cff`](CITATION.cff), which GitHub offers as *Cite this
repository*. Each release is also archived on Zenodo with a DOI of its own.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and pull requests are welcome.

## License

MIT — see [LICENSE](LICENSE).

The module graph is drawn with [React Flow](https://reactflow.dev) (MIT), whose attribution
badge the tool hides on the canvas; this is where it is credited.

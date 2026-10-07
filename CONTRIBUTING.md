# Contributing to Arch Anchor

Thanks for your interest. Arch Anchor started as research tooling and is now maintained as a
regular open-source project — issues and pull requests are both welcome.

## Getting set up

You need **Java 17+** and **Node.js 18+**. Maven is not required; `api/` bundles the wrapper.

```bash
git clone git@github.com:erick-escape/arch-anchor.git
cd arch-anchor

# terminal 1
cd api && ./mvnw spring-boot:run

# terminal 2
cd web && npm install && npm run dev
```

Then open http://localhost:5173 and analyze `examples/sample-architecture-project` — its README
lists the metrics and recommendations you should see, so it doubles as a check that your
environment is sound.

## Before you open a pull request

```bash
cd api && ./mvnw test          # 99 tests; 21 are skipped without the local uploads/ fixture
cd web && npm run lint && npm test && npm run build
```

`./mvnw test` also runs `spring-javaformat:validate`, so a formatting slip fails the build.
`./mvnw spring-javaformat:apply` fixes it. On the frontend, `npm run format` runs Prettier.

## Code style

The conventions are written down in [CLAUDE.md](CLAUDE.md) and enforced by review. The short
version:

- Functions 4–20 lines, files under 500. Split when they grow past that.
- One responsibility per function, one per module.
- Explicit types everywhere. No `any` in TypeScript, no untyped parameters in Java.
- Specific names. If a name gives you more than five grep hits, it is too generic — `data`,
  `handler`, and `Manager` are the usual offenders.
- Early returns over nesting; two levels of indentation is the ceiling.
- Exception messages name the offending value and the expected shape.
- Comments explain *why*. Reference an issue or commit when a line exists because of a specific
  bug.
- Inject dependencies through constructors and parameters, not globals or static imports.

## Tests

- Every new function gets a test; every bug fix gets a regression test.
- Tests must be F.I.R.S.T. — fast, independent, repeatable, self-validating, timely. In
  particular, **independent**: do not add tests that depend on data in `api/uploads/`, which is
  untracked local state. `JavaParserTypeResolutionTest` does, which is why it skips itself on a
  fresh clone.
- Mock external I/O behind named fake classes rather than inline stubs.

## The one thing to understand before touching the analyses

Every structural analysis — split, merge, move — is scored the same way:

```
rate = 0.5 × Δ_similarity + 0.5 × Δ_violations_normalized
```

Only a positive rate is ever recommended, meaning a refactoring must improve cohesion without
costing more violations than it saves. If you change how similarity or violations are counted,
you change the output of all three analyses at once, and `SplitModuleTest`, `MergeModuleTest`,
and `MoveClassTest` are what tell you whether that was intentional.

Also: **`JavaParserService` must stay generic.** It cannot contain special cases for particular
libraries or frameworks. The tool is meant to parse any Java project — Spring Boot, plain Maven,
JavaEE, no framework at all — and a hardcoded shortcut for one ecosystem is a bug even when it
makes a specific project work better.

## Workflow

1. Every change starts from an issue. If there is none for what you want to do, open one.
2. Branch from an up-to-date `main`: `<type>/<issue>-<slug>`, e.g. `fix/1-typed-state`.
3. Write the failing test first, then the change that makes it pass.
4. Open a pull request into `main` and link the issue with `Closes #N` (or `Refs #N` when the
   pull request is one of several on the same issue).
5. It is merged once CI is green.

## Commits

Commit messages are written in English. The history uses a `type: subject` prefix, some of it
with gitmoji. New commits should keep the prefix and drop the emoji:

```
feat: add enforcement modes per reference class
refactor: separate ref class election from similarity computation
fix: correct violation count after a module merge
docs: explain the rating mechanism
test: cover split with a single-class module
chore: bump the JavaParser version
```

Write the body to explain *why* the change is needed, not what the diff shows.

## Reporting bugs

Open an issue with the Java project that triggered it (or a minimal reproduction), what you
expected, and what happened. Analysis bugs are much easier to act on with the module and
reference class names included — and a small synthetic project in the shape of
`examples/sample-architecture-project` is the most useful reproduction of all.

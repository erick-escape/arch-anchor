# Arch Anchor — API

Spring Boot 3.3 backend (Java 17). Parses Java sources with JavaParser, groups classes into
modules, elects reference classes, and computes similarity, violations, and refactoring
recommendations.

See the [root README](../README.md) for what the tool does, the concepts behind it, and the
full endpoint reference.

## Commands

| Command | Description |
|---|---|
| `./mvnw spring-boot:run` | Start the API on `http://localhost:8080` |
| `./mvnw test` | Run all tests |
| `./mvnw test -Dtest=ClassName` | Run one test class |
| `./mvnw clean compile` | Compile |
| `./mvnw clean package` | Build the executable JAR |

Java 17+ is the only prerequisite — the Maven wrapper is bundled.

## Local state

`uploads/`, `modules.bin`, and `project-analyses.bin` are runtime state, all gitignored. Delete
them to reset. The `.bin` files are Java-serialized and therefore tied to the current class
names; a stale one will fail to load, and deleting it is the fix.

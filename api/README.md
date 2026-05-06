# Arch Rationale

### Think about your project in a different way.

[//]: # (For further reference, please consider the following sections:)

[//]: # ()

[//]: # (* [Setup]&#40;link&#41;)

## Setup

### Prerequisites

- Java 17+
- Maven (the project ships with the Maven Wrapper, so no local Maven installation is required)

### Running the project

```bash
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080`.

### Other commands

| Command                | Description                                        |
|------------------------|----------------------------------------------------|
| `./mvnw clean compile` | Compile the project                                |
| `./mvnw test`          | Run all tests                                      |
| `./mvnw clean package` | Build the executable JAR                           |
| `./mvnw clean install` | Install the artifact in the local Maven repository |
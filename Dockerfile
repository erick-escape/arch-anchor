# syntax=docker/dockerfile:1
#
# Arch Anchor as one process on one port: the backend serves the built frontend (#77).
#
#   docker build -t arch-anchor .
#   docker run --rm -p 8080:8080 arch-anchor                     # then open http://localhost:8080
#   docker build --target jar --output type=local,dest=out .     # only the jar, as releases carry it
#
# The build stages run on the builder's own platform, since what they produce (static files and
# a jar) does not depend on it. Only the last stage is per platform, and it only copies, so an
# arm64 image is built without emulation.

FROM --platform=$BUILDPLATFORM node:20-bookworm-slim AS web
WORKDIR /src/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk-jammy AS api
WORKDIR /src/api
COPY api/ ./
COPY --from=web /src/web/dist/ src/main/resources/static/
# The tests run in CI (ci-api.yml); this stage only packages.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B --no-transfer-progress package -DskipTests \
    && cp target/arch-anchor-api-*.jar /arch-anchor.jar \
    && mkdir /data

FROM scratch AS jar
COPY --from=api /arch-anchor.jar /

FROM eclipse-temurin:17-jre-jammy
LABEL org.opencontainers.image.title="Arch Anchor" \
      org.opencontainers.image.description="Recovers a Java codebase's architectural constraints from one reference class per module" \
      org.opencontainers.image.source="https://github.com/erick-escape/arch-anchor" \
      org.opencontainers.image.licenses="MIT"
COPY --from=api /arch-anchor.jar /app/arch-anchor.jar
# The backend keeps its state (uploads/, modules.bin and the JSON stores) in its working
# directory, so that is /data, owned by the unprivileged user the tool runs as.
COPY --from=api --chown=10001:10001 /data /data
USER 10001:10001
WORKDIR /data
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/arch-anchor.jar"]

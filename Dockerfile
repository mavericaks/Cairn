# ═══════════════════════════════════════════════════════════════════
# Cairn Dockerfile — Multi-Stage Build (E1-T5)
# ═══════════════════════════════════════════════════════════════════
# WHY multi-stage: Keeps the final image small (~200MB vs ~800MB).
# Build stage has JDK + Maven (~600MB) — none of that ships to prod.
# Final image has only JRE + the application layers.
#
# WHY layered JAR: Spring Boot splits the JAR into four layers:
#   1. dependencies        (rarely change — cached by Docker)
#   2. spring-boot-loader  (rarely changes)
#   3. snapshot-dependencies (change with snapshot deps)
#   4. application         (changes every build)
# Docker caches layers 1-3, so rebuilds only re-copy layer 4 (~1MB).
# ═══════════════════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────────────────
# WHY eclipse-temurin: Official Adoptium JDK — the de facto standard
# for production Java containers. Alpine variant keeps build small.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# WHY: Copy Maven Wrapper and POM first. Docker caches this layer
# so dependency downloads only re-run when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY pom.xml pom.xml

# WHY: Make mvnw executable (git on Windows may strip permissions).
RUN chmod +x mvnw

# WHY: Download all dependencies in a separate layer.
# -B = batch mode (no interactive progress). -q = quiet.
# This layer is cached until pom.xml changes.
RUN ./mvnw dependency:go-offline -B -q

# WHY: Now copy source code. This layer invalidates on every code change,
# but the dependency layer above stays cached.
COPY src/ src/

# WHY: Build the fat JAR. -DskipTests because tests run in CI (E1-T6),
# not during Docker build. Keeps build fast and deterministic.
RUN ./mvnw package -B -q -DskipTests

# ── Stage 2: Extract Layers ──────────────────────────────────────
# WHY: Spring Boot's layertools extracts the JAR into four directories.
# Copying them individually into the runtime image lets Docker cache
# each layer separately.
FROM eclipse-temurin:21-jdk-alpine AS layers

WORKDIR /extract

# WHY: Copy the built JAR from the builder stage.
COPY --from=builder /build/target/*.jar app.jar

# WHY: Extract the layered JAR into separate directories.
# Spring Boot 3.5.x changed the syntax from `-Djarmode=layertools` to
# `-Djarmode=tools extract --layers --launcher`. Using the new syntax
# avoids the deprecation warning and is forward-compatible with Boot 4.x.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# ── Stage 3: Runtime ─────────────────────────────────────────────
# WHY eclipse-temurin:21-jre-alpine: JRE-only image (~200MB).
# No JDK, no Maven, no build tools. Minimal attack surface.
FROM eclipse-temurin:21-jre-alpine AS runtime

# WHY: Non-root user is a container security requirement.
# UID 1001 avoids conflicts with system users.
# 'cairn' group and user for clear audit trail in logs.
RUN addgroup -S cairn && adduser -S cairn -G cairn -u 1001

WORKDIR /app

# WHY: Activates 'prod' Spring profile inside containers. This switches
# logback-spring.xml (E1-T4) console output from human-readable to structured
# JSON — required for log aggregators (Splunk, Datadog, ELK).
# Set as ENV (not ARG) so it persists at runtime. Can be overridden by
# Railway env vars or `docker run -e SPRING_PROFILES_ACTIVE=...`.
ENV SPRING_PROFILES_ACTIVE=prod

# WHY: Copy layers in order of change frequency (least → most).
# Docker caches unchanged layers — only the application layer
# (which changes every build) is re-copied on typical rebuilds.
# NOTE: jarmode=tools (Boot 3.5+) extracts into an 'app/' subdirectory.
COPY --from=layers /extract/app/dependencies/ ./
COPY --from=layers /extract/app/spring-boot-loader/ ./
COPY --from=layers /extract/app/snapshot-dependencies/ ./
COPY --from=layers /extract/app/application/ ./

# WHY: Create logs directory owned by non-root user.
# Matches LOG_PATH default in logback-spring.xml (E1-T4).
RUN mkdir -p logs && chown -R cairn:cairn /app

# WHY: Switch to non-root user for all runtime operations.
USER cairn

# WHY: Expose the default port. Railway overrides via PORT env var.
# This is documentation for humans and tools — not a security boundary.
EXPOSE 8080

# WHY: JVM container-aware settings.
# MaxRAMPercentage=75: Use 75% of container memory for heap,
#   leaving 25% for metaspace, native memory, and OS overhead.
# UseContainerSupport: JVM reads cgroup memory/CPU limits (default since JDK 10+,
#   but explicit for documentation).
# Spring Boot launcher: org.springframework.boot.loader.launch.JarLauncher
#   is the entry point for layered JARs (Spring Boot 3.2+).
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseContainerSupport", \
    "org.springframework.boot.loader.launch.JarLauncher"]

# PROJECT_JOURNAL.md — Cairn Source of Truth

> This file is the memory of Project Cairn.
> Every session reads it at boot. Every task updates it at completion.
> If this file is missing or empty, the AI must STOP and alert the user immediately.
> Do NOT proceed without it.

---

## Project Identity

| Field | Value |
|-------|-------|
| Project Name | Cairn |
| One-line definition | An extensible Spring Boot AI orchestration platform — each Epic adds one demonstrable enterprise capability to a stable, never-rewritten core |
| Built for | Interview showcase + deep personal learning |
| Working Directory | b:\AKSHAT\SpringAI |
| Tech Stack | Java 21, Spring Boot 3.x, Spring Modulith, Spring AI, PostgreSQL + pgvector, Maven, React (Vite) |
| Frontend philosophy | Built once, extended never rewritten. One professional dark dashboard. |
| LLM Strategy | Free-tier multi-model (Gemini, Groq/Llama-3, Mistral) via round-robin API key interceptor |
| Embedding Strategy | Local MiniLM (384-dim) via Transformers — zero network latency, zero cost |
| Budget Constraint | 100% FREE TIER ONLY for all external services |
| Project Started | 2026-06 (clean restart) |
| Last Updated | 2026-06-13 (Session 2) |

---

## The Four Laws (Never Negotiable)

1. **The Core Is Sacred** — never deleted and rebuilt, only extended or refactored
2. **One Epic, One Capability** — each Epic = one enterprise concept, one interview story
3. **UI Is Infrastructure** — built once, extended only when backend exposes new data
4. **No Complexity Theater** — named for what it does, not how impressive it sounds

---

## Salvaged Decisions from Previous Build

> These are validated technical decisions from the previous project that are worth keeping.
> They are NOT carried over as code — they are carried over as confirmed architectural choices.

| Decision | Justification | Source Epic |
|----------|--------------|-------------|
| pgvector + HNSW indexing for semantic routing | Proven to work, ~20ms routing, free tier | Epic 2 (old) |
| Local MiniLM 384-dim embeddings | Bypasses Gemini embedding endpoint limitations, zero latency | Epic 2 (old) |
| Round-robin API key interceptor for LLM calls | Bypasses free-tier rate limits cleanly | Epic 2 (old) |
| Java 21 Virtual Threads via `@EnableAsync` | Proven in production, correct for agent concurrency | Epic 3 (old) |
| Spring Modulith for module boundaries | Enforces architectural purity, correct for extensibility goal | Epic 1 (old) |
| Flyway for schema migrations | Enterprise standard, gives us versioned DB history | Epic 1 (old) |
| SLF4J + Logback JSON structured logging | Required for CI/CD, Splunk/Datadog compatible | Epic 7 (old) |
| Multi-stage Dockerfile (eclipse-temurin:21-jre-alpine) | Hardened, minimal production container | Epic 7 (old) |
| GitHub Actions CI pipeline | Enforces build integrity on every PR | Epic 7 (old) |
| Spring AI `ChatClient` with exponential backoff retry | Handles Gemini 503 free-tier limits gracefully | Epic 3 (old) |

---

## Discarded Decisions from Previous Build

> These are things the old project did that we explicitly do NOT carry forward.

| Decision | Why Discarded |
|----------|--------------|
| "Agent OS" / "Nexus Intelligence" framing | Costume, not architecture. No clear user value. |
| UI rebuilt 6+ times (glassmorphism, cyberpunk, etc.) | Complexity theater. UI is infrastructure, built once. |
| Epics named "Ultimate", "Redemption", "Omni" | Performance for the AI, not value for the user. |
| Python script to bulk-generate 18 boilerplate files | Shortcuts that bypassed understanding. TDD means writing tests with intention. |
| Epics numbered out of order (6,7,8,9,10,11,12,13,14,16,17,2,3) | Loss of narrative. Epics must be sequential and traceable. |
| Mock domain tools (stockPriceFunction returning fake data) | Dishonest architecture. Tools must do real things or not exist. |
| Supabase full stack | Hides auth/API logic that belongs in Spring. Avoided correctly. |

---

## Epic Roadmap

> The canonical sequence. Epics are added only when the user approves.
> Each Epic has one capability, one interview story.

| Epic | Title | Capability Demonstrated | Status |
|------|-------|------------------------|--------|
| 1 | The Foundation | Spring Modulith structure, Flyway migrations, CI/CD pipeline, structured logging, Docker | ⚪ NOT STARTED |
| 2 | The Semantic Kernel | pgvector + MiniLM local embeddings + HNSW semantic routing + Redis context cache | 🟢 ACTIVE |
| 3 | The Agent Swarm | Virtual Thread agent orchestration, SwarmAgent interface, first real agent | ⚪ NOT STARTED |
| 4 | Observability | Micrometer + Actuator metrics, structured event bus, live dashboard panel | ⚪ NOT STARTED |
| 5 | Agentic Tools (Safe) | Spring AI function calling with HITL gate, sandboxed file system tools | ⚪ NOT STARTED |
| 6 | Security Hardening | JWT auth, Spring Security, HITL approval API for destructive agent actions | ⚪ NOT STARTED |
| 7 | The LoRA Swarm | Multi-model orchestration of 6 domain-specific fine-tuned local GGUF models via Ollama dynamic loading | ⚪ NOT STARTED |
| 8+ | [User-approved additions] | [Defined when we get here] | ⚪ FUTURE |

> **Note:** Epics 1-7 are the spine. Each one is a real interview talking point.
> Epic 8 onward is where the platform becomes truly endless — new capabilities
> plug into the stable foundation without touching what came before.

---

## Architecture Decision Record (ADR)

> Every structural decision gets an ADR. Written before the implementation code.

### ADR-001: Spring Modulith for Module Boundaries
- **Date:** Session 0, updated Session 1 (E1-T2)
- **Decision:** Use Spring Modulith to enforce hard module boundaries between six capability domains: Routing, Model, Agents, Tools, Observability, and Security.
- **Alternatives:** Plain packages (no enforcement), microservices (overkill for free-tier)
- **Reason:** Modulith gives us compile-time architectural enforcement without the operational overhead of microservices. Each module maps to one or two Epics' domain.
- **Trade-offs:** Modulith is opinionated about event publishing — we accept this constraint.
- **Update (E1-T2):** `model` module added to separate LLM client management (HOW to call) from routing (WHERE to send). Prevents `routing` from spanning Epic 2 and Epic 7.
- **Files:** `src/main/java/com/cairn/` — module root. Six modules: `routing`, `model`, `agents`, `tools`, `observability`, `security`.

### ADR-002: Local MiniLM Embeddings
- **Date:** [Session 1]
- **Decision:** Use local Transformers (MiniLM, 384-dim) for all embedding operations.
- **Alternatives:** Gemini embedding API (missing OpenAI-compatible endpoint on free tier), OpenAI (paid)
- **Reason:** Zero network latency (~20ms routing proven), zero cost, no API key dependency for core routing logic.
- **Trade-offs:** Embedding quality ceiling lower than large hosted models. Acceptable for routing use case.
- **Files:** `SemanticRouter.java`

### ADR-003: Round-Robin API Key Interceptor for LLM
- **Date:** [Session 1]
- **Decision:** Use an HTTP interceptor (`AiConfig.java`) to rotate through multiple free-tier API keys on every LLM request.
- **Alternatives:** Single key with rate limit handling (fragile), paid tier (violates budget constraint)
- **Reason:** Proven approach from previous build. Cleanly bypasses per-key rate limits.
- **Trade-offs:** Requires multiple API keys to be provisioned by user. Key management is manual.
- **Files:** `AiConfig.java`

### ADR-004: Redis Cloud as External Domain Context Cache
- **Date:** Session 1 (decision made outside session, recorded here)
- **Decision:** Use Redis (Redis Cloud free tier) as an external domain context cache. Cache sits between domain selection and LLM call.
- **Alternatives:** Caffeine (JVM-bound, lost on restart — stampede risk against free-tier APIs on cold start)
- **Reason:** Decouples cache from JVM lifecycle. Prevents cold-start stampedes against free-tier APIs. Protects token budget across restarts.
- **Trade-offs:** External dependency adds network hop (~1-2ms). Redis Cloud free tier has 30 MB limit. Acceptable for context cache use case.
- **Dependency:** `spring-boot-starter-data-redis`
- **Files:** Epic 2 — Redis context cache implementation

### ADR-005: Railway as Deployment Platform
- **Date:** Session 1 (decision made outside session, recorded here)
- **Decision:** Deploy Cairn to Railway. Production stack: Spring Boot + PostgreSQL + Redis, all on Railway.
- **Alternatives:** Fly.io (more config overhead), Render (slower free-tier cold starts), AWS/GCP (overkill for demo, not free)
- **Reason:** Supports Spring Boot + PostgreSQL + Redis in one place. Free tier sufficient for demo. Auto-deploys from GitHub. Zero infrastructure management overhead.
- **Trade-offs:** Vendor lock-in on deployment config (mitigated by Docker + standard Spring Boot). Free tier has resource limits (512 MB RAM, usage-based billing after trial credits).
- **Files:** `railway.toml` (E1-T9)

### ADR-006: React Served from Spring Boot Static Resources
- **Date:** Session 1 (decision made outside session, recorded here)
- **Decision:** React frontend is built via `npm run build`, output placed in `src/main/resources/static`. Served directly from the Spring Boot JAR.
- **Alternatives:** Separate frontend deployment (Vercel/Netlify), reverse proxy (nginx)
- **Reason:** Single deployment URL. Zero CORS configuration. Simpler demo setup. One process to manage in production.
- **Trade-offs:** Frontend and backend must be deployed together — no independent frontend releases. Acceptable for a demo/interview project.
- **Files:** `src/main/resources/static/` (populated by React build)

### ADR-007: All Environment-Specific Values as Environment Variables
- **Date:** Session 1 (decision made outside session, recorded here)
- **Decision:** Every credential, URL, and environment-specific value is externalized as an environment variable from the first line it is needed. Local: `.env` file. Production: Railway environment variables.
- **Alternatives:** Spring profiles with hardcoded values per profile, config server (overkill)
- **Reason:** Zero code change between local and production environments. No hardcoded credentials or URLs anywhere in the codebase. Ever.
- **Trade-offs:** Requires discipline — every new config value must be added to `.env.example` and Railway dashboard simultaneously.
- **Files:** `.env.example` (E1-T10), `application.yml`

### ADR-008: Testcontainers for Integration Test Database
- **Date:** Session 2 (E1-T7)
- **Decision:** Use Testcontainers to manage disposable PostgreSQL+pgvector containers during test execution. Tests manage their own database lifecycle — no external DB required.
- **Alternatives:** (A) CI-only PostgreSQL service container with manual local setup, (B) H2 in-memory DB for tests (incompatible — pgvector extension doesn't exist in H2), (C) Shared Docker Compose DB for tests (stale state between runs)
- **Reason:** Our Flyway V1 migration runs `CREATE EXTENSION IF NOT EXISTS vector` which requires real PostgreSQL+pgvector. Testcontainers provides identical DB behavior in CI, local dev, and IDE test runs. Spring Boot's `@ServiceConnection` auto-configures the datasource — zero manual config. Replaces the CI PostgreSQL service container for single source of truth.
- **Trade-offs:** Requires Docker to be running during test execution. First test run downloads the pgvector Docker image (~400MB). Subsequent runs use cached image.
- **Dependencies:** `spring-boot-testcontainers` (test scope), `org.testcontainers:postgresql` (test scope), `org.testcontainers:junit-jupiter` (test scope). All test-only — zero production impact.
- **Files:** `src/test/java/com/cairn/TestcontainersConfig.java`, `pom.xml` (test deps)
### ADR-009: Domain-Specific LLM Swarm (Ollama + Student Cloud)
- **Date:** Session 3 (Pre-Epic 2 Planning)
- **Decision:** Shift from cloud API routing (Gemini/Groq) to a 100% private, locally orchestrated swarm of 6 distinct small models (e.g., Qwen-Math, Phi-3, Hermes) running via Ollama dynamic loading on a constrained 4GB VRAM GPU. Fine-tuning will occur on cloud GPUs funded by .edu Student Pack credits (AWS/Azure) using Unsloth, exported as quantized GGUFs for local inference.
- **Alternatives:** Hosting 6 LoRAs in the cloud (violates budget), using generic APIs (lacks ML engineering depth).
- **Reason:** Demonstrates profound ML engineering depth (LoRA, quantization, orchestration) while adhering to strict local hardware limits (4GB VRAM only allows sequential dynamic loading) and $0 ongoing hosting costs.
- **Trade-offs:** 1-3 second latency penalty when Ollama swaps models in/out of constrained VRAM.

### ADR-010: The 6 Foundational Semantic Domains
- **Date:** Session 3 (Pre-Epic 2 Planning)
- **Decision:** Define exactly 6 foundational domains for intent classification: `system`, `conversational`, `discovery`, `execution`, `generative`, and `analytical`.
- **Reason:** A minimal, collectively exhaustive taxonomy that covers every possible enterprise AI prompt format. Provides the target labels for the Epic 2 Semantic Router.

### ADR-011: DomainSeeder ApplicationRunner
- **Date:** Session 3 (Pre-Epic 2 Planning)
- **Decision:** Seed foundational domains into PostgreSQL at application startup using a Java `ApplicationRunner` (`DomainSeeder`) rather than a raw Flyway SQL script.
- **Alternatives:** Flyway V2 migration for `INSERT INTO domains...`
- **Reason:** The `domains` table requires a 384-dimensional vector `embedding`. Raw SQL cannot calculate embeddings from text descriptions. The `DomainSeeder` injects the domains by calling `LocalEmbeddingService` at startup to accurately compute the 384-dim vectors before persisting via JPA/JDBC.
---

## Dependency Registry

| Dependency | Version | Purpose | Date Added | ADR |
|------------|---------|---------|------------|-----|
| spring-boot-starter-parent | 3.5.15 | Parent POM — manages all Spring Boot dependency versions | E1-T1 | — |
| spring-modulith-bom | 1.4.12 | BOM for Spring Modulith module versions | E1-T1 | ADR-001 |
| spring-ai-bom | 1.1.7 | BOM for Spring AI module versions | E1-T1 | ADR-003 |
| spring-boot-starter-web | managed | Core web layer (embedded Tomcat + Spring MVC) | E1-T1 | — |
| spring-modulith-starter-core | managed | Module boundary enforcement | E1-T1 | ADR-001 |
| spring-ai-starter-model-openai | managed | LLM ChatClient — OpenAI-compatible API (pointed at Gemini) | E1-T1 | ADR-003 |
| spring-boot-starter-data-jpa | managed | JPA + Hibernate for DB access | E1-T1 | — |
| postgresql | managed | PostgreSQL JDBC driver (runtime scope) | E1-T1 | — |
| flyway-core | managed | Schema migration versioning | E1-T1 | — |
| flyway-database-postgresql | managed | PostgreSQL dialect support for Flyway 10.x | E1-T1 | — |
| logback-classic | managed (transitive) | Structured JSON logging — pulled in by starter-web | E1-T1 | — |
| deep-java-library (MiniLM) | TBD | Local embeddings — deferred to Epic 2 | — | ADR-002 |
| pgvector | TBD | Vector similarity extension — deferred to Epic 2 | — | ADR-002 |
| spring-boot-starter-data-redis | managed | External domain context cache — decouples cache from JVM lifecycle (Epic 2) | Session 1 | ADR-004 |
| spring-boot-testcontainers | managed (test) | Testcontainers + Spring Boot @ServiceConnection auto-config | E1-T7 | ADR-008 |
| testcontainers-postgresql | managed (test) | PostgreSQL container module for Testcontainers | E1-T7 | ADR-008 |
| testcontainers-junit-jupiter | managed (test) | JUnit 5 lifecycle integration for Testcontainers | E1-T7 | ADR-008 |
| spring-boot-starter-actuator | managed | Production-ready endpoints (/actuator/health for Railway, /info for build metadata) | E1-T9 | ADR-005 |

---

## Open Questions

| ID | Question | Raised | Resolved | Answer |
|----|----------|--------|----------|--------|
| Q-001 | Exact Spring Boot version to pin? (3.3.x vs 3.4.x) | Session 0 | Session 1 (E1-T1) | 3.5.15 — latest GA on 3.x line. Spring AI 2.0 (for Boot 4.x) is RC-only, so we stay on Boot 3.x. |
| Q-002 | Exact Spring AI version compatible with chosen Spring Boot? | Session 0 | Session 1 (E1-T1) | Spring AI 1.1.7 — latest GA for Boot 3.x. Note: starters renamed from `spring-ai-openai-spring-boot-starter` to `spring-ai-starter-model-openai` in 1.1.x. |
| Q-003 | What is Redis used for in Cairn? (Session store? Cache? Message broker?) | Session 1 | Session 1 | External domain context cache. Sits between domain selection and LLM call. Prevents cold-start stampedes against free-tier APIs. See ADR-004. |

---

## Bug Log

| ID | Description | Root Cause | Fix | Session |
|----|-------------|-----------|-----|---------|
| — | — | — | — | — |

---

## Rule Changes

| Date | Change | Reason |
|------|--------|--------|
| Project restart | Rules 0-13 established | Clean restart of Cairn |
| 2026-06-12 | Rule 14 added | Enforce env vars for all credentials/URLs/env-specific values (ADR-007) |
| 2026-06-13 | Rule 15 added | No Scope Reduction by Deference — AI must ask for user prerequisites in PRE-GATE, not silently reduce task scope. Born from E1-T6 revert (Session 2). |
| 2026-06-15 | Rule 16 added | Production-Grade First — Enforces handling of edge cases, graceful degradation on infrastructure failure, and metrics tracking from day one. Born from user rejection of E2-T1 happy-path implementation (Session 3). |

---

## Session Log

> One entry per session. Honest, brief, specific.

### Session 0 — Project Restart
- **Model used:** [Claude Sonnet 4.6 — planning session, no code written]
- **What was accomplished:** Full project restart. Identity defined as "Cairn". Four Laws established. Epic roadmap defined (Epics 1-7 as spine). Salvaged validated decisions from previous build. Discarded complexity theater patterns.
- **Decisions made:** Project name, spine philosophy, epic sequence, all workspace rules
- **What was left incomplete:** Session 1 (Epic 1 Foundation) not yet started
- **Next session must start with:** `Boot Cairn. Read BOOT_PROTOCOL.md and execute the boot sequence.` Then begin Epic 1 Task 1.

### Session 1 — E1-T1 Complete, Architecture Decisions Recorded
- **Date:** 2026-06-12
- **Model used:** Claude Opus 4.6 (Thinking)
- **What was accomplished:**
  - E1-T1 ✅ — Maven project initialized with Spring Boot 3.5.15, Spring Modulith 1.4.12, Spring AI 1.1.7
  - Maven Wrapper (3.9.9) added — no global Maven installation needed
  - Discovered Spring AI 1.1.x renamed starters (`spring-ai-openai-spring-boot-starter` → `spring-ai-starter-model-openai`) — verified against actual BOM POM from Maven Central
  - Fixed auto-config exclusions — Spring AI 1.1.x splits OpenAI auto-config into 6 separate classes
  - Q-001 resolved (Spring Boot 3.5.15), Q-002 resolved (Spring AI 1.1.7), Q-003 resolved (Redis = domain context cache)
  - Working directory corrected from `SpringBoot_Project` to `SpringAI`
  - ADR-004 (Redis Cloud context cache), ADR-005 (Railway), ADR-006 (React from static), ADR-007 (env vars) recorded
  - Rule 14 added (env vars from line one)
  - E1-T9, E1-T10, E1-T11 added to Epic 1 board. E2-T1 added to Epic 2 backlog.
  - E1-T2 ✅ — Six Spring Modulith modules created: `routing`, `model`, `agents`, `tools`, `observability`, `security`. Added `model` module (user-approved) to separate LLM management from routing. ADR-001 updated.
  - E1-T3 ✅ — Flyway + PostgreSQL configured. V1 migration: pgvector extension + `domains` table with `vector(384)` + HNSW cosine index. Datasource uses env vars with local defaults (Rule 14). DB auto-config exclusions removed.
  - E1-T4 ✅ — Logback JSON structured logging. Uses Spring Boot 3.5.x built-in `StructuredLogEncoder` (zero external deps). Console: readable local / JSON prod via `<springProfile>`. File: always JSON with 10MB/30d/1GB rotation. `.gitignore` created.
- **Decisions made:** Version pinning (Boot 3.5.15 over 4.x due to Spring AI compatibility), deployment platform (Railway), env var strategy, Redis purpose, 6-module structure (added `model`)
- **What was left incomplete:** E1-T5 through E1-T11 not yet started
- **Next task:** E1-T5 — Create multi-stage Dockerfile

### Session 2 — E1-T5 Complete, E1-T6 Reverted then Completed (Rule 6 Lesson)
- **Date:** 2026-06-13
- **Model used:** Claude Opus 4.6 (Thinking)
- **What was accomplished:**
  - E1-T5 ✅ — Multi-stage Dockerfile reviewed, corrected, and validated via `docker build`
  - Existing Dockerfile (created in Session 1 but ungated) was reviewed rather than rewritten (Law 1)
  - Added `ENV SPRING_PROFILES_ACTIVE=prod` to runtime stage — containers now activate prod profile for structured JSON console logging (E1-T4 integration)
  - Discovered Spring Boot 3.5.x deprecated `java -Djarmode=layertools -jar app.jar extract` in favor of `java -Djarmode=tools -jar app.jar extract --layers --launcher` — fixed
  - Discovered new `jarmode=tools` extracts into an `app/` subdirectory — updated all four COPY --from=layers paths accordingly
  - `.dockerignore` reviewed — comprehensive and correct, no changes needed
  - Docker image validated: 199 MB (JRE-only), non-root `cairn` user (UID 1001), `SPRING_PROFILES_ACTIVE=prod` set
  - **Rule 6 Violation & Recovery:** AI proceeded with E1-T6 without explicit PRE-GATE approval (answering an open question ≠ approval). User ordered full revert. All E1-T6 work (ci.yml, git init, .gitattributes, commits) reverted. Rule 15 added. Task restarted with proper gate.
  - **Rule 15 added:** No Scope Reduction by Deference — AI must flag USER ACTION REQUIRED prerequisites in PRE-GATE instead of silently reducing scope.
  - E1-T6 ✅ (redo, proper gate) — GitHub Actions CI pipeline created and verified green
  - Git repo initialized (`git init -b main`), remote added (`origin` → `https://github.com/mavericaks/Cairn.git`), pushed to GitHub
  - `.gitattributes` added for cross-platform line ending normalization
  - CI pipeline: 2-job workflow (build-and-test → docker-build). PostgreSQL+pgvector service container. Java 21 Temurin + Maven caching. Test report archival. Docker image verification (non-root, prod profile). Concurrency groups.
  - CI verified green on first push: Build & Test (53s) + Docker Build (1m 57s) = 3m 1s total
  - No new dependencies introduced
- **Decisions made:** Prod profile set via ENV in Dockerfile. Adopted new `jarmode=tools` syntax. PostgreSQL service container in CI (Option A — honest, not mocked). Used pgvector/pgvector:pg17 image. GitHub repo: mavericaks/Cairn.
- **What was left incomplete:** E1-T8 through E1-T11 not yet started
- **Next task:** E1-T9 — Create `railway.toml` deployment configuration

#### E1-T7 Details (added to Session 2)
- E1-T7 ✅ — Smoke tests with Testcontainers
- ADR-008 written: Testcontainers for integration test database
- **3 test classes, 10 tests, all passing:**
  - `CairnApplicationTests` (4 tests): context load, app name, Flyway V1 migration (pgvector extension + domains table), virtual threads
  - `ModuleStructureTests` (2 tests): Spring Modulith boundary verification (all 6 modules detected), architecture doc generation
  - `LoggingConfigTests` (4 tests): Logback backend, file appender, context health, log file path
- `TestcontainersConfig`: shared PostgreSQL+pgvector container via `@ServiceConnection`
- CI migrated from service container to Testcontainers (Option B — single source of truth)
- Hibernate explicit dialect removed (HHH90000025 fix — auto-detected since Hibernate 6.x)
- CI green: Build & Test (49s) + Docker Build (1m 49s) = 2m 50s
- Dependencies added (all test scope): `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`, Testcontainers BOM 1.20.6

#### E1-T9 Details (added to Session 2)
- E1-T9 ✅ — Railway deployment configuration
- `railway.toml` created: DOCKERFILE builder, health check at /actuator/health (30s timeout), ON_FAILURE restart (3 retries), smart watchPatterns
- `spring-boot-starter-actuator` added to pom.xml — health + info endpoints exposed (whitelist, show-details: always)
- Actuator config in application.yml — minimal attack surface (only health + info)
- Health endpoint smoke test added to CairnApplicationTests (HTTP 200 + status:UP) — 11 tests total, all passing
- CI green: Build & Test (53s) + Docker Build (3m 19s)

#### E1-T10 Details (added to Session 2)
- E1-T10 ✅ — `.env.example` with all environment variable keys
- 3 required vars (DB_URL, DB_USERNAME, DB_PASSWORD), 3 optional (PORT, LOG_PATH, SPRING_PROFILES_ACTIVE), 3 future (OPENAI_API_KEY, OPENAI_BASE_URL, REDIS_URL)
- Each var has format example, Railway behavior note, and ADR/task cross-reference
- `.env` confirmed in `.gitignore` (E1-T4)
- No code changes, no tests needed — documentation file only

#### E1-T11 Details (added to Session 2)
- E1-T11 ✅ — `docker-compose.yml` for local dev environment
- Mirrors the Railway production stack locally
- Defined `postgres` service using `pgvector/pgvector:pg17` (matches CI and prod) on port 5432
- Defined `redis` service using `redis:7-alpine` on port 6379 with requirepass (password)
- Named volumes configured for both `postgres-data` and `redis-data` to ensure persistence
- Both services have healthchecks
- App is deliberately excluded from compose to allow local IDE/Maven running

#### E1-T8 Details (added to Session 2)
- E1-T8 ✅ — `walkthrough_epic1.md` written
- Comprehensive retrospective of Epic 1
- Documented "The Why" for Java 21, Spring Modulith, PostgreSQL, pgvector, Flyway, and Testcontainers
- Explained the CI/CD pipeline, multi-stage Dockerfile, and local parity
- Epic 1 is now 100% complete
- Epic 1 is now 100% complete

### Session 3 — The Swarm Pivot & Epic 2 Activation
- **Date:** 2026-06-15
- **Model used:** Gemini 3.1 Pro (High)
- **What was accomplished:** 
  - Successfully recovered from IDE context loss using `BOOT_PROTOCOL.md` and explicit mental model verification.
  - Pivoted project LLM strategy from "Generic API Routing" to "Local Domain-Specific LoRA Swarm" based on user's exact specifications (NVIDIA GTX 1650 4GB VRAM + student cloud credits).
  - Designed the 6 foundational domains taxonomy (ADR-010).
  - Identified critical architectural pivot: Domain seeding requires a Spring `ApplicationRunner` to compute 384-dim embeddings at runtime instead of a raw Flyway script (ADR-011).
  - Epic 2 (The Semantic Kernel) formally activated.
  - E2-T1 ✅ — Implemented `DomainContextCacheService` in the `routing` module. Used `StringRedisTemplate` with a 1-hour TTL to isolate context by `userId` and `domain`. Added Redis container (`GenericContainer(redis:7-alpine)`) to `TestcontainersConfig` via `@ServiceConnection`. Wrote `DomainContextCacheServiceTest` which passed alongside the full suite (15/15 tests passing).
- **Decisions made:** ADR-009, ADR-010, ADR-011 established. Epic 7 renamed to "The LoRA Swarm".
- **What was left incomplete:** E2-T2 through E2-T6
- **Next task:** E2-T2 — Add DJL dependencies and implement LocalEmbeddingService (MiniLM 384-dim).

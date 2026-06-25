# task.md — Cairn Task Board

> **The gate is the contract.**
> PRE-GATE must be confirmed before work starts.
> POST-GATE must be confirmed before a task is marked ✅.
> Both must be visible in chat. No exceptions.

---

## Current Epic

| Field | Value |
|-------|-------|
| Epic Number | 2 |
| Epic Title | The Semantic Kernel |
| Capability | Local zero-cost semantic intent classification (DJL + pgvector) and Redis context caching |
| Interview Story | "I built a semantic router that classifies intent in 15ms locally, completely bypassing the massive cost and latency of asking an LLM to decide which agent should handle the prompt." |
| Status | 🟢 ACTIVE |
| Definition of Done | `SemanticKernelTests` integration test passes using Testcontainers, successfully embedding a prompt via DJL, finding the correct domain via pgvector, and caching the context in Redis. |

---

## Epic 2 Task Board

| ID | Task | Status |
|----|------|--------|
| E2-T1 | Implement Redis context cache with TTL per domain (ADR-004) | ✅ |
| E2-T2 | Add DJL dependencies and implement LocalEmbeddingService (MiniLM 384-dim) (ADR-002) | ✅ |
| E2-T3 | Create DomainSeeder ApplicationRunner to inject the 6 foundational domains | ✅ |
| E2-T4 | Harden LocalEmbeddingService (thread safety, float[] return, @ConfigurationProperties) | ✅ |
| E2-T5 | Expand DomainSeeder with Example Queries (Few-Shot pgvector indexing) | ✅ |
| E2-T6 | Implement DomainRouter (Native pgvector HNSW search) | ⚪ |
| E2-T7 | Write Testcontainers integration test combining Postgres + Redis + DJL | ⚪ |
| E2-T8 | Update PROJECT_JOURNAL.md and write walkthrough_epic2.md | ⚪ |

---

## Epic 7 Task Board (Parallel Side-Quest)
> **Note:** We are executing Epic 7 in parallel with the Java backend to speed up the massive ML training workload.
| ID | Task | Status |
|----|------|--------|
| E7-T1 | Write `model.py`: Pure PyTorch Llama-3 architecture | ⚪ |
| E7-T2 | Write `loader.py`: HuggingFace safetensors state dict mapping | ⚪ |
| E7-T3 | Write `lora.py`: Custom Low-Rank Adaptation injection | ⚪ |
| E7-T4 | Write `train.py`: The PyTorch training loop | ⚪ |
| E7-T5 | Write `export.py`: Merge adapters and compile to GGUF | ⚪ |
| E7-T6 | Orchestrate dynamic loading in Spring Boot | ⚪ |

---

## Task Gate Template

> Copy this for every task. Fill every field. Paste into chat before starting and before marking done.

```
╔══════════════════════════════════════════════════════════════╗
║                     CAIRN TASK GATE                          ║
╠══════════════════════════════════════════════════════════════╣
║  Task ID    : [e.g. E1-T3]                                   ║
║  Task Title : [one line]                                     ║
║  Epic       : [Epic N — Title]                               ║
╠══════════════════════════════════════════════════════════════╣
║                  PRE-GATE (before work begins)               ║
╠══════════════════════════════════════════════════════════════╣
║  [ ] BOOT_PROTOCOL.md was read this session                  ║
║  [ ] PROJECT_JOURNAL.md was read this session                ║
║  [ ] This task is consistent with the Four Laws              ║
║  [ ] This task is consistent with existing ADRs              ║
║  [ ] My assumptions for this task are:                       ║
║      1. ...                                                  ║
║      2. ...                                                  ║
║  [ ] User has confirmed assumptions above                    ║
║  [ ] Definition of Done is clear and agreed                  ║
╠══════════════════════════════════════════════════════════════╣
║                  POST-GATE (before marking ✅)               ║
╠══════════════════════════════════════════════════════════════╣
║  [ ] Code compiles — `mvn clean install` passes              ║
║  [ ] JUnit test written and passing for this task            ║
║  [ ] Zero System.out.println in all new/changed code         ║
║  [ ] All public methods have Javadoc with WHY sentence       ║
║  [ ] All non-trivial lines have WHY comments                 ║
║  [ ] No unaddressed TODOs in committed code                  ║
║  [ ] No new dependency without a corresponding ADR entry     ║
║  [ ] No architectural decision made silently                 ║
║  [ ] PROJECT_JOURNAL.md updated for this task                ║
║  [ ] task.md status updated                                  ║
╠══════════════════════════════════════════════════════════════╣
║  PRE-GATE  : [ ] CONFIRMED BY USER                           ║
║  POST-GATE : [ ] CONFIRMED BY USER                           ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Completed Tasks Archive

| ID | Task | Completed | Notes |
|----|------|-----------|-------|
| E1-T1 | Initialize Maven project with Java 21 + Spring Boot 3.5.15 + Spring Modulith 1.4.12 + Spring AI 1.1.7 | 2026-06-12 | Maven Wrapper included. Spring AI starter renamed to `spring-ai-starter-model-openai`. Q-001/Q-002 resolved. |
| E1-T2 | Define Spring Modulith module structure (6 modules: routing, model, agents, tools, observability, security) | 2026-06-12 | Added `model` module (user-approved) to separate LLM management from routing. ADR-001 updated. |
| E1-T3 | Configure Flyway + PostgreSQL + V1 migration (pgvector + domains table) | 2026-06-12 | Datasource/JPA/Flyway configured with env var defaults (Rule 14). DB auto-config exclusions removed. HNSW index on vector(384). |
| E1-T4 | Configure SLF4J + Logback JSON structured logging with rotating file output | 2026-06-12 | Uses Spring Boot 3.5.x built-in StructuredLogEncoder (no external dep). Console: readable local / JSON prod. File: JSON, 10MB/30d/1GB. .gitignore created. |
| E1-T5 | Create multi-stage Dockerfile (eclipse-temurin:21-jre-alpine) | 2026-06-13 | 3-stage build (builder→layers→runtime). Fixed Boot 3.5.x jarmode deprecation (layertools→tools). Added SPRING_PROFILES_ACTIVE=prod ENV. Image: 199MB, non-root cairn user. |
| E1-T6 | Create GitHub Actions CI pipeline (ci.yml — build, test, Docker build) | 2026-06-13 | 2-job workflow: build-and-test (pgvector/pgvector:pg17, Maven caching, test reports) → docker-build (image verification). Git init + remote (mavericaks/Cairn). .gitattributes added. CI green on first push (3m 1s). Rule 15 born from this task. |
| E1-T7 | Write smoke tests (context, modules, logging) with Testcontainers | 2026-06-13 | 3 classes, 10 tests. CairnApplicationTests (context, Flyway, pgvector), ModuleStructureTests (6 modules verified), LoggingConfigTests (Logback, file appender). Testcontainers @ServiceConnection (ADR-008). CI migrated from service container to Testcontainers. |
| E1-T9 | Create railway.toml deployment configuration (ADR-005) | 2026-06-13 | DOCKERFILE builder, /actuator/health check (30s), ON_FAILURE restart (3 retries), smart watchPatterns. Actuator added (health+info only). Health endpoint smoke test. 11 tests total. |
| E1-T10 | Create .env.example with all env var keys (ADR-007) | 2026-06-14 | 3 required (DB_*), 3 optional (PORT, LOG_PATH, SPRING_PROFILES_ACTIVE), 3 future (OPENAI_*, REDIS_URL). Format examples, Railway notes, ADR cross-refs. |
| E1-T11 | Create docker-compose.yml for local dev | 2026-06-14 | Postgres (pgvector/pgvector:pg17) and Redis (redis:7-alpine). Named volumes and healthchecks for both. Spring Boot app excluded from compose for IDE running. |
| E1-T8 | Write walkthrough_epic1.md | 2026-06-14 | Extensive architectural deep-dive into Epic 1. Explained the "Why" for Java 21, Modulith, PostgreSQL, pgvector, Flyway, and infrastructure. |
| E2-T1 | Implement Redis context cache with TTL per domain (ADR-004) | 2026-06-15 | Enforced Rule 16 (Production-Grade First). Handled null inputs, graceful degradation on Redis downtime, and added Micrometer tracking for hit/miss/failure. Testcontainers integration test covers all edge cases. |
| E1-T12 | Enterprise Hardening (Spotless, GlobalExceptionHandler, Validation, Swagger, Rate Limiting) | 2026-06-15 | Enforced static code quality, centralized error handling, input validation, API documentation, and DoS protection. |
| E2-T2 | Add DJL dependencies and implement LocalEmbeddingService | 2026-06-18 | Added Deep Java Library to calculate 384-dimensional arrays offline natively on CPU using HuggingFace all-MiniLM-L6-v2. No network requests. |
| E2-T3 | Create DomainSeeder ApplicationRunner | 2026-06-18 | Built Domain entity with hibernate-vector, DomainRepository, and DomainSeeder. Seeded 6 foundational domains at startup, dynamically calculating embeddings to avoid hardcoded tensors. |
| E2-T4 | Harden LocalEmbeddingService | 2026-06-24 | Thread safety fix, float[] return type, type-safe config binding, graceful degradation. |
| E2-T5 | Expand DomainSeeder with Example Queries | 2026-06-25 | Created domain_examples table with HNSW index. Seeded 60 total example queries dynamically. Added relationships to Domain. |

---

## Blocked / Parked Tasks

| ID | Task | Blocked By | Parked |
|----|------|-----------|--------|
| — | — | — | — |

---

## Future Epic Backlog

> Epics proposed but not yet approved. User approves each before it enters the active board.

| Epic | Title | Proposed | Approved |
|------|-------|---------|----------|
| 3 | The Agent Swarm | Session 0 | Pending |
| 4 | Observability | Session 0 | Pending |
| 5 | Agentic Tools (Safe) | Session 0 | Pending |
| 6 | Security Hardening | Session 0 | Pending |
| 7 | The LoRA Swarm | Session 3 | Pending |

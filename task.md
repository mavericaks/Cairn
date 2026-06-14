# task.md — Cairn Task Board

> **The gate is the contract.**
> PRE-GATE must be confirmed before work starts.
> POST-GATE must be confirmed before a task is marked ✅.
> Both must be visible in chat. No exceptions.

---

## Current Epic

| Field | Value |
|-------|-------|
| Epic Number | 1 |
| Epic Title | The Foundation |
| Capability | Spring Modulith project structure, Flyway migrations, CI/CD pipeline, structured logging, Docker |
| Interview Story | "I set up the project with Spring Modulith to enforce hard module boundaries from day one — here's why that matters over plain packages..." |
| Status | ⚪ NOT STARTED |
| Definition of Done | `mvn clean install` passes. Structured JSON logs confirmed. Zero `System.out.println`. Docker image builds. CI pipeline runs on push. Module structure matches ADR-001. |

---

## Epic 1 Task Board

| ID | Task | Status |
|----|------|--------|
| E1-T1 | Initialize Maven project with correct Java 21 + Spring Boot 3.x + Spring Modulith dependencies | ✅ |
| E1-T2 | Define Spring Modulith module structure (`routing`, `model`, `agents`, `tools`, `observability`, `security`) | ✅ |
| E1-T3 | Configure Flyway + PostgreSQL (Docker). Create V1 migration (pgvector extension + base schema) | ✅ |
| E1-T4 | Configure SLF4J + Logback JSON structured logging. logback-spring.xml with rotating file output | ✅ |
| E1-T5 | Create multi-stage Dockerfile (eclipse-temurin:21-jre-alpine) | ✅ |
| E1-T6 | Create GitHub Actions CI pipeline (`ci.yml` — build, test, Docker build on every PR) | ✅ |
| E1-T7 | Write smoke test: ApplicationContext loads, modules are isolated, logging outputs JSON | ✅ |
| E1-T9 | Create `railway.toml` deployment configuration (ADR-005) | ✅ |
| E1-T10 | Create `.env.example` with all required environment variable keys (ADR-007) | ✅ |
| E1-T11 | Create `docker-compose.yml` for local dev environment (Spring Boot + PostgreSQL + Redis) mirroring Railway stack (ADR-005) | ✅ |
| E1-T8 | Write `walkthrough_epic1.md` | ⚪ |

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
| 2 | The Semantic Kernel | Session 0 | Pending Session 1 review |

> **Epic 2 — Known Tasks (preliminary, finalized when Epic 2 is activated):**
>
> | ID | Task | Status |
> |----|------|--------|
> | E2-T1 | Implement Redis context cache with TTL per domain (ADR-004) | ⚪ |
> | ... | [Remaining tasks defined at Epic 2 activation] | — |
| 3 | The Agent Swarm | Session 0 | Pending |
| 4 | Observability | Session 0 | Pending |
| 5 | Agentic Tools (Safe) | Session 0 | Pending |
| 6 | Security Hardening | Session 0 | Pending |
| 7 | Multi-Model Routing | Session 0 | Pending |

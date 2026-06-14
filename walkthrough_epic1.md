# Epic 1 Walkthrough: The Foundation

This document provides a comprehensive retrospective and architectural deep-dive into **Epic 1: The Foundation** of the Cairn project. It details what was built, why specific technologies were chosen over alternatives, and how the "Four Laws of Cairn" guided these decisions.

This is your master reference for the fundamental architecture of the application.

---

## 1. The Philosophical Base: The Four Laws of Cairn

Before writing a single line of code, we established the "Four Laws" to govern all technical decisions in this project. These laws prevent feature creep, technical debt, and architecture astronautics.

1. **The Core is Sacred:** The `cairn` application must remain a single, easily deployable Spring Boot artifact. No microservices, no distributed tracing nightmares. Complexity must earn its keep.
2. **One Epic, One Capability:** We do not build "half a feature" now to support something later. We build exactly what the current Epic demands.
3. **UI is Infrastructure:** The frontend (React) is just another consumer of the core API. The core must never leak UI concerns into its domain logic.
4. **No Complexity Theater:** If a simple `if` statement works, we don't use a Strategy Pattern. If a single database works, we don't add a message broker.

Every decision below was measured against these laws.

---

## 2. The Tech Stack & "The Why"

Here is a detailed breakdown of our foundational technology choices, why we made them, and the alternatives we rejected.

### Why Java 21 & Spring Boot 3.5.x?
* **What it is:** The runtime and web framework for the application.
* **Why we chose it:** Java 21 introduces **Virtual Threads** (Project Loom). For an Agentic AI system that will eventually make hundreds of concurrent HTTP calls to LLMs (Epic 3), traditional OS threads would bottleneck and consume massive memory. Virtual threads allow us to handle massive concurrency with blocking I/O (which Spring AI uses) without complex, hard-to-read reactive programming (WebFlux).
* **Alternatives rejected:**
  * *Node.js / Python:* While popular for AI, Java's static typing, enterprise refactoring tools, and the new Spring AI ecosystem make it far superior for maintaining a large, complex domain model over time.
  * *Spring WebFlux:* Violates "No Complexity Theater". Reactive programming is notoriously hard to debug. Virtual Threads give us the performance of WebFlux with the simplicity of traditional imperative code.

### Why Spring Modulith?
* **What it is:** A toolset that enforces logical boundaries within a single monolithic application.
* **Why we chose it:** It prevents the "Big Ball of Mud" anti-pattern. By organizing code into logical modules (`routing`, `model`, `agents`, `tools`, `observability`, `security`), we get the strict isolation benefits of Microservices (code in `agents` cannot illegally access internal code in `routing`) without the operational nightmare of deploying 6 separate applications. It enforces these rules at test time (via ArchUnit).
* **Alternatives rejected:**
  * *Standard Package-by-Layer:* (e.g., `controllers`, `services`, `repositories`). This inevitably leads to spaghetti code where any controller can talk to any repository.
  * *Microservices:* Violates "The Core is Sacred." Too much DevOps overhead for a project that doesn't yet have Google-scale traffic.

### Why PostgreSQL?
* **What it is:** The primary relational database.
* **Why we chose it:** It is the industry gold standard for relational data. It is ACID compliant, battle-tested, and heavily supported by the Spring ecosystem. More importantly, it supports extensions, which leads us to our next choice.
* **Alternatives rejected:**
  * *MySQL / MariaDB:* Lacks the robust extension ecosystem (specifically for vector search) that PostgreSQL has natively.
  * *MongoDB / NoSQL:* Our domain model (Users, Projects, Agents, Tasks) is highly relational. NoSQL would force us to implement joins and transaction management in the application code, which is error-prone.

### Why pgvector?
* **What it is:** An open-source vector similarity search extension for PostgreSQL.
* **Why we chose it:** Agentic AI requires storing and querying vector embeddings (e.g., "find documents similar to this prompt"). By using `pgvector`, our relational data (Users) and vector data (Embeddings) live in the **exact same database**. We can do ACID-compliant joins across standard data and vector data.
* **Alternatives rejected:**
  * *Pinecone, Weaviate, Milvus:* Dedicated vector databases. Rejected because they violate "The Core is Sacred" and "No Complexity Theater." Introducing a separate vector DB means managing two databases, handling distributed transactions, and dealing with data synchronization issues. `pgvector` gives us 90% of the performance with 1% of the operational complexity.

### Why Flyway?
* **What it is:** A database migration tool. It tracks schema changes in versioned SQL files (e.g., `V1__base_schema.sql`).
* **Why we chose it:** It treats database schemas like source code. When another developer (or CI server) spins up the app, Flyway guarantees the database looks exactly as expected. We can reliably recreate the production database structure locally.
* **Alternatives rejected:**
  * *Hibernate `ddl-auto=update`:* Never use this in production. Hibernate tries to guess how to alter your database based on Java classes. It frequently drops columns or creates inefficient indices. Flyway gives us explicit, reviewable SQL control over the database schema.

### Why Testcontainers?
* **What it is:** A Java library that automatically spins up real Docker containers (like Postgres) during JUnit tests, and tears them down when tests finish.
* **Why we chose it:** "Test like you fly." Testcontainers ensures our tests run against the exact same database engine (PostgreSQL 17 with pgvector) that runs in production. If a SQL query works in tests, it is guaranteed to work in production.
* **Alternatives rejected:**
  * *H2 In-Memory Database:* H2 is a Java database. It does not support `pgvector`. It also handles certain SQL syntax differently than Postgres. Passing tests on H2 often fail on Postgres in production. Testcontainers eliminates this entire class of bugs.

---

## 3. Infrastructure & Deployment

A robust application is useless if it cannot be deployed reliably. Epic 1 established a professional CI/CD and deployment pipeline.

### The Multi-Stage Dockerfile
We created a 3-stage Dockerfile (`builder` -> `layers` -> `runtime`).
* **Why:** Spring Boot creates "Fat JARs" that bundle all dependencies. If you change one line of code, Docker would normally have to upload a 50MB+ image. By using Spring Boot's layer tools, our Dockerfile separates dependencies (which rarely change) from application code (which changes often).
* **Result:** Blazing fast Docker builds and minimal storage overhead, packaged securely in an Alpine JRE container running as a non-root user.

### The GitHub Actions Pipeline (CI)
Our `ci.yml` runs on every pull request and push to `main`.
* **Why:** It enforces a strict gate. Before code is merged, CI spins up a fresh Ubuntu machine, downloads PostgreSQL via Testcontainers, runs all our tests, and verifies the Dockerfile builds successfully.
* **Rule 15:** This step birthed "Rule 15" — if an action is required by the user (like creating a repo), the AI must flag it upfront.

### Railway Deployment & Local Parity
We chose Railway (ADR-005) for production hosting due to its excellent support for Spring Boot, PostgreSQL, and Redis in a single unified dashboard, with auto-deployments from GitHub.
* `railway.toml`: We codified our deployment settings (health checks, builder type, watch patterns) so they are version-controlled, rather than hidden in a web UI.
* `docker-compose.yml`: We recreated the exact Railway backing services (PostgreSQL + Redis) for local development.
* `.env.example`: We mapped out every environment variable (DB credentials, API keys) the app will ever need, ensuring no hardcoded secrets exist in the codebase.

---

## 4. Epic 1 Retrospective

In Epic 1, we did not write a single business logic feature. However, we achieved something far more important: **A professional, indestructible foundation.**

We now have:
1. A Java 21 / Spring Boot 3.5 app capable of massive virtual-thread concurrency.
2. A strictly enforced modular architecture (`routing`, `model`, `agents`, etc.).
3. A robust, version-controlled PostgreSQL database with vector search capabilities.
4. Structured JSON logging ready for production observability.
5. An automated CI pipeline that tests everything against real containerized databases.
6. A production-ready Docker deployment configuration.

With this foundation set, we are completely unblocked to begin Epic 2 (LLM API Integrations), knowing the infrastructure underneath us is rock solid.

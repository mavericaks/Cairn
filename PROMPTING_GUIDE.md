# PROMPTING_GUIDE.md — The Complete Operating Manual

> **Purpose:** This guide ensures ANY model (Gemini, Claude, GPT) follows the exact same workflow
> without deviation. It defines the session lifecycle, gate system, teaching protocol, and
> every prompt you'll need. Read it once. Refer back when something feels off.

---

## Document Map — What Lives Where

| Document | Location | Purpose |
|----------|----------|---------|
| `STARTUP_PROMPT.md` | Project root | Paste as FIRST message in every new session |
| `BOOT_PROTOCOL.md` | Project root | Initialization sequence — model reads project state |
| `WORKSPACE_RULES.md` | Project root | 13 operating rules with violation indicators |
| `PROJECT_JOURNAL.md` | Project root | Session history, ADRs, architecture decisions |
| `task.md` | Project root | Active Epic, task board, progress tracking |
| `cairn_technical_specification.md` | Project root | Complete tech spec — schema, APIs, stack, data flows |
| `implementation_plan.md` | Artifacts dir | Master execution plan — every task with file-level instructions |
| `PROMPTING_GUIDE.md` | Project root | This file — workflow, gates, teaching, prompts |

**The model MUST read these files in order at session start.** If it skips any, the session is compromised.

---

## The Golden Rule of Prompting

**The AI will do exactly what you reward it for doing.**
If you say "great job!" after it skips a test — it will skip tests again.
If you say "you violated Rule 7, fix it" — it learns the boundary is real.
Your responses to the AI are as important as your prompts to it.

---

## Session Lifecycle (The Full Flow)

Every session follows this exact sequence. No shortcuts.

```
┌─────────────────────────────────────────────┐
│  1. BOOT — Paste STARTUP_PROMPT.md          │
│     → Model reads all project files          │
│     → Model outputs CAIRN BOOT block         │
│     → Model outputs SESSION RESUME SUMMARY   │
│     → Model says "Cairn is ready."           │
├─────────────────────────────────────────────┤
│  2. TASK SELECTION — You say which task      │
│     "Let's work on E2-T4."                   │
├─────────────────────────────────────────────┤
│  3. PRE-GATE — Model outputs the gate block  │
│     → You review assumptions                 │
│     → You approve or correct                 │
├─────────────────────────────────────────────┤
│  4. LEARN — Model teaches concepts           │
│     → CONCEPT BRIEF block output             │
│     → You read, ask questions                │
│     → You say "Concept understood."          │
├─────────────────────────────────────────────┤
│  5. EXECUTE — Model writes code + tests      │
│     → Follows file-level instructions         │
│     → Follows SDE Non-Negotiable Standards   │
│     → mvn spotless:apply + mvn verify        │
├─────────────────────────────────────────────┤
│  6. POST-GATE — Model outputs completion gate│
│     → You verify all checkboxes              │
│     → You approve                            │
├─────────────────────────────────────────────┤
│  7. COMMIT — Git branch + push + merge       │
│     → task.md updated                        │
│     → PROJECT_JOURNAL.md updated             │
└─────────────────────────────────────────────┘
```

---

## Phase 1: Boot

### Your prompt:
```
Boot Cairn. Read BOOT_PROTOCOL.md and execute the boot sequence.
```

### What you must see:
```
=== CAIRN BOOT ===
Model      : [model name]
Date       : [today's date]
Boot file  : BOOT_PROTOCOL.md ✅ read
```

AND:

```
=== SESSION RESUME SUMMARY ===
Project          : Cairn
Current Epic     : [number and title]
Last completed   : [task ID + description]
Active tasks     : [list]
Blocked tasks    : [list or "none"]
Open questions   : [list or "none"]
Core constraints : [the 4 Laws]
==============================
```

### If boot fails:
```
Boot failed. I don't see the boot block and session summary. Start over.
```

---

## Phase 2: Task Selection

### Your prompt:
```
Let's work on [E2-T4]. Start with the PRE-GATE.
```

### What the model must do:
1. Read the task's file-level instructions from `implementation_plan.md`
2. Output the PRE-GATE block

---

## Phase 3: PRE-GATE

### What you must see:
```
=== PRE-GATE: [Task ID] ===
Task         : [one-line description]
Goal         : [what this task achieves]
Files        : [list of files to create/modify]
Layers       : [which of the 13 layers this touches]
Dependencies : [which tasks must be complete first]
Assumptions  : [numbered list — user must confirm each]
SDE Standards: [which of the 10 standards apply]
================================
```

### Your response options:

**If all correct:**
```
Assumptions confirmed. PRE-GATE approved. Proceed to LEARN phase.
```

**If an assumption is wrong:**
```
Assumption 2 is wrong. [Correct version]. Update and reconfirm.
```

---

## Phase 4: LEARN (Teaching Protocol)

### What the model must do:
Output a CONCEPT BRIEF for EVERY concept in the task's "Concepts to Master" list.

```
=== CONCEPT BRIEF: [Task ID] ===
Concepts     : [list]
Why It Matters: [why this concept exists]
How It Works  : [step by step]
Key Insight   : [the non-obvious thing]
Tradeoffs     : [gain vs lose]
Edge Cases    : [what could go wrong]
================================
```

### Your response options:

**If you understand:**
```
Concept understood. Proceed to code.
```

**If you want deeper teaching:**
```
I don't understand [specific concept]. Teach me using the 9-stage flow.
```

The model must then follow the full teaching protocol:
1. Diagnose what you already know
2. Frame the problem
3. Show the naive/baseline approach
4. Build the key insight
5. Explain full mechanics
6. Walk through a concrete example
7. Cover edge cases
8. Discuss tradeoffs and alternatives
9. Verify your understanding with questions

**The model does NOT proceed to code until you confirm understanding.**

### If model skips LEARN phase:
```
You skipped the LEARN phase. Output the CONCEPT BRIEF before writing code.
This is mandatory per the execution plan.
```

---

## Phase 5: EXECUTE

### Rules the model must follow:
1. Follow file-level instructions from `implementation_plan.md` exactly
2. Every new Java class: Javadoc, `// WHY:` comments, SLF4J logger
3. Every service method: `@Audited` annotation (once built), input validation
4. Every REST endpoint: DTO input/output, `@Valid`, proper HTTP status, pagination
5. `mvn spotless:apply` before commit
6. `mvn verify` — all tests pass
7. No code committed without a test
8. Non-obvious decisions explained with `// WHY:` inline comments

### If model writes code without a test:
```
Where is the test for this class? POST-GATE cannot be approved without tests.
Write it now.
```

### If model exposes a JPA entity directly in a controller:
```
This violates SDE Standard #1. Entities never leave the service layer.
Create a DTO and mapper. Fix this before proceeding.
```

---

## Phase 6: POST-GATE

### What you must see:
```
=== POST-GATE: [Task ID] ===
Files Created  : [list with paths]
Files Modified : [list with paths]
Tests Added    : [list with names + what they verify]
Tests Passing  : [X/X all green]
SDE Compliance : [which standards applied]
Layer Coverage : [which layers addressed]
Build Status   : mvn verify ✅
Spotless       : applied ✅
================================
```

### Your response options:

**If everything checks out:**
```
POST-GATE approved. Mark [Task ID] as ✅ in task.md and update PROJECT_JOURNAL.md.
```

**If something is missing:**
```
POST-GATE item [X] is not satisfied. Show me the evidence before I approve.
```

---

## Phase 7: Commit + Journal

### What the model must do after POST-GATE approval:
```bash
git checkout -b feat/[task-id]
git add . && git commit -m "[Task ID]: [description]"
git push origin feat/[task-id]
# CI passes
git checkout main && git merge feat/[task-id] && git push origin main
```

Then:
1. Update `task.md` — mark task ✅
2. Update `PROJECT_JOURNAL.md` — add session entry

---

## Deviation Prevention Prompts

Keep these ready. Paste when needed.

### Scope creep:
```
This task is [E2-T4]. We are not building anything outside that scope.
Complete the current task gate before we discuss anything new.
```

### Complexity theater:
```
This is over-engineered. What is the simplest implementation that
satisfies the POST-GATE for [Task ID]?
```

### Missing test:
```
Where is the JUnit test for this class? POST-GATE cannot be approved
without a test. Write it now.
```

### Silent decision:
```
You introduced [X] without an ADR. Write the ADR entry in
PROJECT_JOURNAL.md first, then we decide together.
```

### Entity leak:
```
You're returning a JPA entity from a controller. This violates SDE
Standard #1. Create a DTO and mapper.
```

### Skipped teaching:
```
You skipped the LEARN phase. Output the CONCEPT BRIEF before writing code.
```

### Model drift:
```
Session drift detected. Show me the current state of PROJECT_JOURNAL.md
session log and the active task gate. If you can't — reboot.
```

### New dependency without ADR:
```
Stop. Write the ADR entry for [dependency] in PROJECT_JOURNAL.md first.
I need: what it is, why chosen, alternatives considered, trade-offs.
```

---

## SDE Non-Negotiable Standards Quick Reference

Every task must comply with whichever of these apply:

| # | Standard | One-line rule |
|---|----------|--------------|
| 1 | DTO Layer | Entities never leave the service layer |
| 2 | Pagination | Every list endpoint uses `Pageable` + `Page<T>` |
| 3 | @ConfigurationProperties | Every external service gets type-safe config |
| 4 | Custom AOP Annotation | `@Audited` on service methods via AOP |
| 5 | Exception Hierarchy | `CairnException` → domain-specific subclasses |
| 6 | Spring Profiles | `application-dev.yml` + `application-prod.yml` |
| 7 | @Scheduled Cleanup | At least one scheduled maintenance job |
| 8 | State Machine | `ToolStatus` transitions enforced in code |
| 9 | @Cacheable | Hot paths cached via Spring Cache + Redis |
| 10 | Complex JPA Query | Multi-join with pagination and dynamic filtering |

---

## 13-Layer Quick Reference

| # | Layer | Check |
|---|-------|-------|
| L1 | Frontend | React, Vite, TypeScript |
| L2 | APIs & Backend | Spring Boot, REST, SSE, DTOs |
| L3 | Database & Storage | PostgreSQL, pgvector, Flyway, MinIO |
| L4 | Auth & Permissions | OAuth2, JWT |
| L5 | Hosting & Deployment | Docker, Railway |
| L6 | Cloud & Computing | Ollama, DJL |
| L7 | CI/CD | GitHub Actions |
| L8 | Security & RBAC | CORS, CSP, HITL |
| L9 | Rate Limiting | Bucket4j + Redis |
| L10 | Caching | Redis, @Cacheable |
| L11 | Load Balancing & Scaling | K8s, HPA, Helm |
| L12 | Error Tracking & Logs | Logback, Micrometer, Zipkin |
| L13 | Availability & Recovery | Resilience4j, health checks |

---

## Interview Prep Prompts (Use After Each Epic)

```
I want to practice explaining Epic [N] in an interview.
Ask me the three hardest technical questions, one at a time.
```

```
What are the two most impressive things about Epic [N] that I should
lead with in an interview?
```

```
What trade-offs did we accept in Epic [N] and how would I defend them
to a senior engineer?
```

---

## What NOT to Say

| Don't say | Say instead |
|-----------|-------------|
| "Just do it quickly" | "Complete the task gate for this." |
| "You're doing great, keep going" | [Review the gate, then approve explicitly] |
| "We can add tests later" | "Tests are part of the POST-GATE. Write them now." |
| "Let's try something different" | "What specific problem are we solving? Create a task." |
| "Can we just refactor everything?" | "What specific problem? Propose an extension, not a rewrite." |
| "Skip the comments" | "Comments are non-negotiable per Rule 7." |
| "I don't care, just pick" | "I need to understand this. Teach me first." |

---

## What TO Say (Power Prompts)

| Situation | Prompt |
|-----------|--------|
| Start of session | `Boot Cairn. Read BOOT_PROTOCOL.md and execute the boot sequence.` |
| Start a task | `Let's work on [E2-T4]. Start with the PRE-GATE.` |
| Approve PRE-GATE | `Assumptions confirmed. PRE-GATE approved. Proceed to LEARN phase.` |
| Approve learning | `Concept understood. Proceed to code.` |
| Approve POST-GATE | `POST-GATE approved. Mark [Task ID] as ✅ in task.md and update PROJECT_JOURNAL.md.` |
| Request teaching | `I don't understand [X]. Teach me using the 9-stage flow.` |
| End session | `Save everything. Update PROJECT_JOURNAL.md with today's session.` |
| Check status | `Show me the current task gate and PROJECT_JOURNAL.md session log.` |
| Propose new Epic | `Propose what Epic [N] should be based on task.md and the roadmap.` |
| Enforce rule | `This violates Rule [N]. Do not proceed. Fix it.` |

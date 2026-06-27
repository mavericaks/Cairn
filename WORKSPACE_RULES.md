# WORKSPACE_RULES.md — Cairn Operating Contract

> Version: 2.0 (Project restart — clean slate)
> Every rule has an Enforcement Mechanism and a Violation Indicator.
> Violation Indicators are observable facts, not feelings. If you see one, stop the AI.

---

## RULE 0 — Boot Protocol Supersedes Everything
Every session begins with `BOOT_PROTOCOL.md`. No exceptions.
- **Enforcement:** First AI response must contain `=== CAIRN BOOT ===` block.
- **Violation:** Any first response without the boot block. Stop and say: "Boot failed. Start over."

---

## RULE 1 — Zero Assumptions
If anything is ambiguous — technology choice, schema design, API contract, naming —
the AI stops and asks before writing code.
- **Enforcement:** AI must list assumptions explicitly in the PRE-GATE before any task starts. User confirms.
- **Violation:** AI writes code that makes a structural choice without a prior confirmation in that session.

---

## RULE 2 — The Core Is Sacred (The First Law)
The foundational architecture is never deleted and rebuilt. Extended or refactored only.
Applies to: module structure, DB schema baseline, routing contract, API shape, logging setup.
- **Enforcement:** Any PR/change that touches a core file must reference the ADR in PROJECT_JOURNAL.md that justifies it.
- **Violation:** An Epic deletes a working component and replaces it entirely. Flag it. Propose extension instead.

---

## RULE 3 — One Epic, One Capability (The Second Law)
Every Epic = one clearly named enterprise concept, explainable in one sentence.
New Epics are proposed by the AI only when the user asks. The user approves before work starts.
- **Enforcement:** Epic title must be in the format: `Epic N: [Technical Capability Name]` — no superlatives.
- **Violation:** Epic touches more than one architectural layer under a single vague title.
  Split it. Log both in task.md separately.

---

## RULE 4 — UI Is Infrastructure (The Third Law)
Frontend is built once to a professional standard. It is extended with new panels/data only.
UI-only Epics are banned. An Epic may include frontend work only if a new backend
data contract requires new UI surface.
- **Enforcement:** Every frontend change must reference the backend endpoint/DTO it is visualizing.
- **Violation:** An Epic whose primary deliverable is visual. Reject it. Ask: "What new backend
  capability does this visualize?" If the answer is "nothing new" — it's not an Epic.

---

## RULE 5 — No Complexity Theater (The Fourth Law)
Epic titles, variable names, and architecture decisions must be named for what they do,
not how impressive they sound.
Banned words in Epic titles: Ultimate, Omni, Redemption, Overhaul, Premium, Winning,
Supreme, Master, Revolution, Rebirth, Reborn, Hyper, Mega, Next-Gen.
- **Enforcement:** AI refuses to name or rename anything with banned words. User may not override this.
- **Violation:** A title containing any banned word. Rename before proceeding.

---

## RULE 6 — Task Gate Is Mandatory
No task starts without a completed PRE-GATE. No task is done without a completed POST-GATE.
Both must appear visibly in the chat. See task.md for the template.
- **Enforcement:** AI pastes filled gate into chat. User confirms PRE before work, reviews POST before ✅.
- **Violation:** Task marked done without a visible POST-GATE in the same session.

---

## RULE 7 — Production Code Standards (Non-Negotiable from Day 1)
All code must meet these standards from the very first line written:
- SLF4J/Logback structured JSON logging. `System.out.println` is permanently banned.
- Every public method has Javadoc with `@param`, `@return`, and a WHY sentence.
- Every new class has a corresponding JUnit 5 test file created in the same task.
- No unaddressed `TODO` comments in any committed code.
- All comments explain WHY, not WHAT.
- **Enforcement:** POST-GATE checklist includes every item above. Cannot be ticked without evidence.
- **Violation:** Any of the above missing when a task is marked done.

---

## RULE 8 — Architecture Requires a Written Justification
No new dependency, pattern, or technology is introduced without:
1. What it is
2. Why it was chosen over alternatives
3. What trade-offs are accepted
4. Which ADR number in PROJECT_JOURNAL.md records this
- **Enforcement:** AI writes the ADR entry before writing the implementation code.
- **Violation:** A new dependency appears in pom.xml without a corresponding ADR entry.

---

## RULE 9 — Backend Dictates Frontend
Frontend is a visualization layer. Zero business logic lives in the frontend.
API contracts are defined in backend DTOs first. Frontend is built to match them.
- **Enforcement:** Every frontend component must reference the backend DTO/endpoint it renders.
- **Violation:** Frontend contains conditional logic that mirrors or duplicates backend rules.

---

## RULE 10 — Journal Is Updated Every Task
PROJECT_JOURNAL.md is updated at the end of every completed task — not just Epics.
Session log entry is mandatory at session end.
- **Enforcement:** POST-GATE includes "PROJECT_JOURNAL.md updated" checkbox. Cannot skip.
- **Violation:** Session ends without a new Session Log entry in PROJECT_JOURNAL.md.

---

## RULE 11 — Radical Honesty, Always
If a requirement is flawed, overly complex, against best practices, or repeating a
past mistake from the old journal — the AI says so directly, explains why, proposes
a better path, and lets the user decide.
The AI never silently implements something it knows is wrong.
- **Enforcement:** If AI flags a concern and user overrides it, AI logs the override and concern in PROJECT_JOURNAL.md.
- **Violation:** AI implements a design it flagged as problematic without logging the user override.

---

## RULE 12 — Agentic Security Is Non-Negotiable
Any Epic that gives an agent access to JVM execution, OS files, DB writes, or
external APIs must implement all four pillars before the Epic is marked complete:
1. Context Sandboxing — isolated, destroyable execution context
2. Hard Timeouts — virtual thread guillotine, no infinite loops
3. HITL Gate — destructive actions require explicit UI approval step
4. Read-Only Default — write access is explicitly granted per task, not assumed
- **Enforcement:** Agentic Epic POST-GATE includes all four pillars as checkboxes.
- **Violation:** Agent tool with write/execute access deployed without all four pillars in place.

---

## RULE 13 — Walkthrough Required for Every Epic Completion
Every completed Epic produces a `walkthrough_epicN.md` file.
Contents: what was built (file by file), why each file exists, key code explained
with WHY comments, how to demo it, known limitations.
- **Enforcement:** Epic is not marked COMPLETE in task.md without the walkthrough file existing.
- **Violation:** Epic marked COMPLETE without a walkthrough file.

---

## RULE 14 — Environment Variables from Line One
Every credential, URL, and environment-specific value must be an environment variable
from the first line it is needed. Hardcoded values are a POST-GATE violation.
Local development uses a `.env` file. Production uses Railway environment variables.
No hardcoded credentials or URLs anywhere in the codebase. Ever.
- **Enforcement:** POST-GATE checklist includes "zero hardcoded credentials/URLs" check. Every new env var must be added to `.env.example` in the same task.
- **Violation:** Any committed code containing a hardcoded API key, database URL, or environment-specific value. Immediate revert required.

---

## RULE 15 — No Scope Reduction by Deference
If a task requires the user to perform an action (create a repo, provision a service,
provide a credential), the AI must ask for it upfront in the PRE-GATE — not silently
reduce the task scope by deferring it. The user and AI are collaborating. The AI's job
is to identify every prerequisite, flag what it needs from the user, and build the
complete solution — not a lesser version that avoids asking.
- **Enforcement:** PRE-GATE must list any user actions required as explicit prerequisites
  with the label `USER ACTION REQUIRED`. The task does not start until the user has
  completed those actions or explicitly deferred them.
- **Violation:** AI writes code that works around a missing prerequisite instead of asking
  for it. AI says "the user will do X later" when X is needed for this task to be complete.

---

## RULE 16 — Production-Grade First (No Happy Path Only)
Code is never built just to "make it work". It must be production-grade from the first line.
This means explicitly handling null inputs, aggressively catching and degrading gracefully on external dependency failures (e.g., Redis down, DB timeout), and adding observability metrics (Micrometer) for every external integration.
- **Enforcement:** Every integration test suite must contain explicit `@Test` methods for failure modes (e.g., `shouldDegradeGracefullyWhenRedisIsDown()`). Code must explicitly check assertions (`Assert.notNull`).
- **Violation:** Delivering a "happy path only" implementation. Failing to handle `null` inputs. Failing to catch and gracefully degrade `RedisConnectionFailureException` or similar external infrastructure exceptions.

---

## RULE 17 — Feature Branch Workflow (No Direct Commits to Main)
All code changes must go through a Pull Request. No direct commits or pushes to `main`. Ever.
- **Branch naming:** `feat/e{epic}-t{task}-short-description` (always lowercase, always with task ID)
- **Commit messages:** `feat(module): E{N}-T{N} description` (Conventional Commits format)
- **Workflow:** Branch → Commit → Push → Open PR → CI passes → Merge via GitHub → Branch auto-deleted
- **Tagging:** Each completed Epic is tagged on `main` as `v0.{epic}.0` (e.g., `v0.4.0`)
- **Enforcement:** `main` branch has GitHub branch protection enabled. PRs require CI to pass before merge.
- **Violation:** Any commit pushed directly to `main` without a PR. Any branch left on remote after merge.

---

## Adding or Modifying Rules
1. Propose the new rule in chat with justification.
2. User approves.
3. Add to this file with a Rule number.
4. Log in PROJECT_JOURNAL.md under `## Rule Changes` with date and reason.
5. Check all active tasks in task.md for conflicts. Flag any found.


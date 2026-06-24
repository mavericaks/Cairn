# BOOT PROTOCOL — Execute This First. Every Session. No Exceptions.

> You are the AI development assistant for **Project Cairn**.
> This is not optional reading. This is your operating initialization sequence.
> Do not write code, do not answer questions, do not suggest anything —
> until this boot sequence is complete and confirmed.

---

## WHAT IS CAIRN (Read this every session — internalize it)

**Cairn** is an extensible Spring Boot platform where each Epic adds exactly one
demonstrable enterprise engineering capability to a stable, never-rewritten core.

A cairn in the real world is a stack of stones built deliberately — one stone at a time,
each placed with intention, the structure never torn down and rebuilt from scratch.
That is the law of this project.

**The core is sacred. It is never rewritten. It is only extended.**

**One-line resume definition:**
> "Cairn is an extensible Spring Boot AI orchestration platform — each module
> independently demonstrable as an enterprise engineering concept."

**Built for:** Interview showcase + deep personal learning in enterprise Spring Boot + AI.

**What a user does on Cairn:** Interacts with a platform that intelligently routes
intent to specialized AI agents, with each Epic adding a new real capability
(not a cosmetic change) to that routing and execution pipeline.

---

## THE FOUR LAWS (Violations are non-negotiable stops)

### LAW 1 — The Core Is Never Rewritten
The foundational architecture (module structure, DB schema baseline, routing contract,
API surface) is never deleted and rebuilt. It is refactored or extended only.
**Violation:** Any Epic that says "delete", "trash", "rebuild from scratch", or
"overhaul" on a component that already works. Stop. Flag it. Propose an extension instead.

### LAW 2 — One Epic, One Capability
Every Epic must add exactly one new, clearly named enterprise concept.
It must be explainable in one sentence to an interviewer.
**Violation:** An Epic that touches more than one architectural layer without a clear
single capability as the headline (e.g. "UI overhaul + new agent + security" = 3 Epics, not 1).

### LAW 3 — UI Is Infrastructure, Not the Product
The frontend is built once to a professional standard and then only extended with
new panels or data — never redesigned. UI Epics are banned unless the backend
exposes a fundamentally new data contract that requires new UI surface.
**Violation:** Any Epic whose primary goal is aesthetic. Aesthetics are handled
once, in the Foundation Epic, and frozen.

### LAW 4 — No Complexity Theater
"Omni", "Ultimate", "Redemption", "Interview-Winning" in Epic titles are banned.
Epics are named after what they technically deliver, not how impressive they sound.
**Violation:** An Epic title containing superlatives, marketing language, or
dramatic reframing of existing work as new work.

---

## BOOT SEQUENCE (Complete in order before any task)

### STEP 1 — State your identity
In your first response output:
```
=== CAIRN BOOT ===
Model      : [your model name]
Date       : [today's date]
Boot file  : BOOT_PROTOCOL.md ✅ read
```

### STEP 2 — Read project state files in this exact order
1. `PROJECT_JOURNAL.md` — decisions, architecture, session log
2. `task.md` — active Epic and task board
3. `WORKSPACE_RULES.md` — operating contract (13 rules)
4. `cairn_technical_specification.md` — complete tech spec (schema, APIs, stack, data flows)
5. `PROMPTING_GUIDE.md` — session workflow, gate system, teaching protocol

If any file is missing: **STOP. Tell the user. Do not proceed.**

### STEP 3 — Output the Session Resume Summary
```
=== SESSION RESUME SUMMARY ===
Project          : Cairn
Current Epic     : [number and title]
Last completed   : [task ID + one line description]
Active tasks     : [list]
Blocked tasks    : [list or "none"]
Open questions   : [list or "none"]
Core constraints : [list the 4 Laws from BOOT_PROTOCOL.md]
==============================
```

### STEP 4 — Wait
Say: "Cairn is ready. What are we building today?"
Do NOT suggest tasks. Do NOT start coding. Do NOT propose new Epics unprompted.

---

## SESSION START COMMAND (User gives this every session)

The user will always begin with:
```
Boot Cairn. Read BOOT_PROTOCOL.md and execute the boot sequence.
```

If the user's first message does not produce a CAIRN BOOT block and a
SESSION RESUME SUMMARY — the boot failed. The user will say "Boot failed, restart."

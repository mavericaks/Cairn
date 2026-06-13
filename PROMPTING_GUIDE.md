# PROMPTING_GUIDE.md — How to Talk to Antigravity

> This is your operating manual for getting correct, consistent, drift-free
> behavior from Antigravity (or any AI) on Project Cairn.
> Read it once. Refer back when something feels off.

---

## The Golden Rule of Prompting

**The AI will do exactly what you reward it for doing.**
If you say "great job!" after it skips a test — it will skip tests again.
If you say "you violated Rule 7, fix it" — it learns the boundary is real.
Your responses to the AI are as important as your prompts to it.

---

## Session Start — Every Single Time

Always begin with this exact phrase:
```
Boot Cairn. Read BOOT_PROTOCOL.md and execute the boot sequence.
```

Wait for the `=== CAIRN BOOT ===` block and the `=== SESSION RESUME SUMMARY ===`.
If you don't see both — say this:
```
Boot failed. I don't see the boot block and session summary. Start over.
```

Do not proceed until the boot is confirmed.

---

## Starting a New Task

Once booted, start a task like this:
```
Let's work on [E1-T3]. Start with the PRE-GATE.
```

The AI must produce the filled PRE-GATE before writing any code.
You read the assumptions. If they're correct:
```
Assumptions confirmed. PRE-GATE approved. Proceed.
```
If an assumption is wrong:
```
Assumption 2 is wrong. [Correct version]. Update and reconfirm.
```

---

## Completing a Task

When the AI says it's done, ask:
```
Show me the POST-GATE before I mark this done.
```

Read every checkbox. If any is unchecked or unsubstantiated:
```
POST-GATE item [X] is not satisfied. Show me the evidence before I approve.
```

Only when every box is ticked and you're satisfied:
```
POST-GATE approved. Mark E1-T3 as ✅ in task.md and update PROJECT_JOURNAL.md.
```

---

## When the AI Proposes Something New

Any time the AI proposes a new dependency, pattern, or architectural change:
```
Stop. Write the ADR entry for this in PROJECT_JOURNAL.md first. Then we discuss.
```

You should see an ADR with: what it is, why it was chosen, alternatives, trade-offs.
If you don't see all four — it's incomplete.

---

## When You Want a New Epic

```
I want to plan the next Epic. Propose what Epic [N] should be based on
the current state in PROJECT_JOURNAL.md and our roadmap.
```

The AI proposes. You approve or modify. It only enters task.md after your approval:
```
Epic [N] approved as proposed. Add it to task.md and PROJECT_JOURNAL.md.
```

---

## When the AI Drifts or Violates a Rule

Use the violation language from WORKSPACE_RULES.md directly:
```
This violates Rule [N] — [paste the violation indicator]. 
Do not proceed. Revert this and propose a compliant approach.
```

Do not accept the work. Do not say "it's fine this once."
Every exception you grant trains the AI that the rules are optional.

---

## When Something Feels Wrong But You Can't Name It

Ask the Golden Check:
```
Show me the current state of PROJECT_JOURNAL.md session log
and the active task gate.
```

If the AI cannot produce both immediately and accurately — it has drifted.
Restart the session:
```
Session drift detected. Reboot. Read BOOT_PROTOCOL.md and restart.
```

---

## The Five Prompts That Prevent Spiraling

Keep these in a text file. Paste them when needed.

**1. Scope creep:**
```
This task is [E1-T3]. We are not building anything outside that scope right now.
Complete the current task gate before we discuss anything new.
```

**2. Complexity theater:**
```
This is over-engineered for what we need right now. What is the simplest
implementation that satisfies the POST-GATE for [E1-T3]?
```

**3. UI drift:**
```
This is a UI change. What new backend data contract does it visualize?
If the answer is nothing new, this is not an Epic. Stop.
```

**4. Missing test:**
```
Where is the JUnit test for this class? The POST-GATE cannot be approved
without a test. Write it now.
```

**5. Silent decision:**
```
You introduced [X] without an ADR. Write the ADR entry first,
then we decide together if we accept it.
```

---

## Prompts for Interview Prep (Use at End of Each Epic)

Once an Epic is complete and walkthrough.md exists:
```
I want to practice explaining Epic [N] in an interview.
Ask me the three hardest technical questions an interviewer
would ask about what we built, one at a time.
```

```
What are the two most impressive things about Epic [N]'s implementation
that I should lead with in an interview answer?
```

```
What trade-offs did we accept in Epic [N] and how would I defend them
to a senior engineer?
```

---

## What NOT to Say

These phrases consistently produce bad AI behavior on long-running projects:

| Don't say | Say instead |
|-----------|-------------|
| "Just do it quickly" | "Complete the task gate for this." |
| "You're doing great, keep going" | [Review the gate, then approve explicitly] |
| "We can add tests later" | "Tests are part of the POST-GATE. Write them now." |
| "Let's try something different for the UI" | "What new backend capability would this visualize?" |
| "Can we just refactor the whole thing?" | "What specific problem are we solving? Create a task for it." |
| "This is taking too long, skip the comments" | "Comments are non-negotiable per Rule 7. Continue." |

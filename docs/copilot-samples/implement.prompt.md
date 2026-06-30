---
mode: agent
model: Claude Opus 4.5
description: Premium implementation from a prepared plan (works from .copilot/plan.md, does not re-crawl)
---

You are the **implement** step. Run this in a FRESH chat so you don't inherit the
explore step's context — that's what keeps your input small and cheap.

Task: ${input:task:What should we implement?}

Context: read `#file:.copilot/plan.md`. Work ONLY from that plan and the files it
names. Do NOT crawl the wider repo or use `@workspace` — if the plan is missing
something, ask one focused question instead of exploring broadly.

Do:
1. Make the edits described in the plan, mirroring the listed conventions.
2. Run the unit-test command from the plan; fix failures.
3. Report concisely: changed files (path:line), and the test result.

Output discipline: no preamble, show only changed lines (never whole files), lead
with the result. Don't summarize unless asked.

---
mode: agent
model: GPT-4.1
description: Cheap exploration — locate the relevant code and write a compact implementation plan (no edits)
tools: ['codebase', 'search']
---

You are the **explore** step. Goal: investigate the task cheaply and hand the next
(premium) step a tiny, high-signal plan. You do NOT implement anything.

Task: ${input:task:What should we implement?}

Steps:
1. Find the parts of the codebase relevant to the task (search by symbol/path; read
   only what you must). Prefer `#sym:` / targeted reads over `@workspace`.
2. Write the plan to `.copilot/plan.md`, OVERWRITING it. Keep it under ~40 lines.

The plan file must contain ONLY:
- **Task:** one line restating the goal.
- **Files to touch:** 3–8 entries, each `path — one-line role / what changes`.
- **Insertion points:** the exact function/class/line area for each change.
- **Conventions:** patterns to mirror (naming, DI, module split, test style).
- **Verification:** the unit-test command to run after implementing.

Hard rules:
- Do NOT paste file contents into the chat or the plan — reference paths/symbols only.
- Do NOT edit any source file. The only file you write is `.copilot/plan.md`.
- Be terse. No preamble, no summary in chat beyond "Plan written to .copilot/plan.md".

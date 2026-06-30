---
description: Delegate a Kotlin Multiplatform task to the specialized kmp-developer agent (project context preloaded)
argument-hint: <task to implement, e.g. "add a wishlist feature module">
---
Use the **kmp-developer** subagent to handle the following task in this Kotlin Multiplatform project.

Task:
$ARGUMENTS

Instructions for delegation:
- Spawn the `kmp-developer` agent and give it the task verbatim plus any relevant detail from the current conversation.
- The agent already knows to read `.claude/docs/STACK.md`, `.claude/docs/ARCHITECTURE.md`, and `CLAUDE.md` for context and to follow this project's conventions (api/real split, Metro DI, per-store flavors, runtime `BrandColorScheme` theming, no strings-branding system).
- Let it implement AND verify (compile the affected flavor) before returning.
- When it finishes, relay its summary — changed files, build result, and anything that needs my decision.

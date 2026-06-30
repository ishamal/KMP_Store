# Copilot Token-Optimization Playbook (Android / Kotlin repo)

**How to use:** In the target repo, open Copilot Chat in **agent mode** with **Claude Opus 4.5**, then paste:

> Read and execute `docs/copilot-token-optimization.md` against this repository. Do the audit first and show me the report before making any edits. Make changes only after I approve.

(Or save this file as `.github/prompts/optimize-tokens.prompt.md` and invoke `/optimize-tokens`.)

---

## IDE note — features differ by IDE

- **VS Code Copilot:** supports prompt files (`/slash` commands), path-scoped `*.instructions.md` with `applyTo`, and the Settings keys below.
- **Android Studio / JetBrains Copilot plugin:** supports `.github/copilot-instructions.md`, the **model picker** (switch per conversation), and **agent mode** (recent versions) — but **NOT** prompt-file slash commands or `applyTo`-scoped instruction files. On JetBrains:
  - Keep ONE lean `.github/copilot-instructions.md` (you can't scope by path) and push detail into `docs/*.md` referenced on demand.
  - Switch models **manually** via the chat model dropdown (base for routine, premium for hard).
  - Run the two-model flow **manually**: base-model chat → paste the explore prompt → it writes `.copilot/plan.md`; then a **new chat**, switch dropdown to premium → paste the implement prompt pointing at the plan. The `explore.prompt.md` / `implement.prompt.md` files are **paste-in snippets**, not auto-commands.
  - Ignore the VS Code Settings keys in step 0.

## Quickstart — steps to run

> Do steps 0–2 on the **base model** (config work shouldn't burn premium requests). Reserve premium models for real implementation later. **Steps marked (VS Code only)** don't apply to the Android Studio plugin — see the IDE note above.

**0. Enable (once, VS Code only):**
- `"chat.promptFiles": true`
- `"github.copilot.chat.codeGeneration.useInstructionFiles": true`
- *Android Studio:* nothing to enable — `.github/copilot-instructions.md` is read automatically. Just confirm the chat panel shows a **model dropdown** (and an **Agent** toggle, if your version has it).

**1. Audit (base model, Ask mode, read-only):**
> Read this file and execute **Phase 1 (Audit) only**. Inventory the instruction/agent/prompt files, report sizes + what's injected every request, and give the before-vs-target token estimate. Do not edit.

**2. Apply (base model, Agent mode):**
> Now execute **Phase 2**: slim the global `copilot-instructions.md`, split rules into `applyTo`-scoped `*.instructions.md` (kotlin / compose / gradle / unit-tests / ui-tests), move long reference text into `docs/*.md` with pointers, add the Output-discipline block, de-duplicate. Show each change as a diff before writing, then re-run the size estimate.

**3. Add the two-model flow:**
- *VS Code:* copy `explore.prompt.md` + `implement.prompt.md` into `.github/prompts/`; fix `model:` names to match your org's picker; add `.copilot/` to `.gitignore`.
- *Android Studio:* keep those two files in `docs/copilot-samples/` as **paste-in snippets**; add `.copilot/` to `.gitignore`. You'll paste the explore body (base model) and implement body (premium model, new chat) manually.

**4. Verify:**
- *VS Code:* type `/` in chat → see `/explore`, `/implement`. Dry-run `/explore`, confirm it writes a small `.copilot/plan.md` (no file dumps).
- *Android Studio:* base-model chat → paste explore snippet → confirm it produces a short plan (and writes `.copilot/plan.md` if agent mode is available). New chat + premium model → paste implement snippet.

**5. Commit** `.github/copilot-instructions.md`, `.github/instructions/*`, `.github/prompts/*`, moved `docs/*`.

**6. Use going forward:** base model for routine/reading; `/explore` → new chat → `/implement` for hard tasks; one task per chat; `/unit-test` as default verification, `/ui-test` only when needed.

---

## Your role

You are auditing how this repository drives GitHub Copilot **token / premium-request** consumption, then refactoring its instruction setup to minimize it **without losing guidance quality**. The dominant cost is content that is injected into *every* request (instruction files) and context that is over-fetched (e.g. `@workspace`, agent file crawling).

Work in two phases: **(1) Audit & report**, then **(2) Refactor on approval**. Do not edit before showing the report.

---

## Phase 1 — Audit (read-only). Produce a report.

1. **Inventory instruction/context files.** Locate and list, with approximate line + token counts (≈ words × 1.3):
   - `.github/copilot-instructions.md`
   - `AGENTS.md` (root and any nested)
   - `.github/instructions/*.instructions.md`
   - `.github/prompts/*.prompt.md`
   - any Android/Kotlin "agent" or rules file the team added.
2. **Flag always-injected weight.** `.github/copilot-instructions.md` and `AGENTS.md` are sent on *every* request. Report their size and call out anything that is: long prose, examples, file dumps, duplicated content, or rules only relevant to a subset of files.
3. **Find path-mismatched rules.** Identify rules that apply only to certain areas (Kotlin/Compose/Gradle/tests/CI) but currently live in the global file — these should be scoped.
4. **Check scoping.** For each `*.instructions.md`, report whether it has `applyTo` frontmatter and whether the glob is as narrow as possible.
5. **Report context-fetch habits** if visible in prompt files or docs: use of `@workspace`, broad agent crawling, missing `#file:`/`#sym:` discipline.

**Output a table:** file | lines | ~tokens | injected-every-request? | problems | recommended action (trim / scope / move-to-docs / merge / delete).
Then estimate **current per-request injected tokens** vs **target after refactor**.

---

## Phase 2 — Refactor (after approval)

### A. Shrink the global file
- Reduce `.github/copilot-instructions.md` (and `AGENTS.md`) to terse, high-signal bullets: project type, languages, build/test commands, hard rules, "do/don't". No prose, no examples, no long lists.
- Move long reference material (architecture, module map, deep conventions) into `docs/*.md` that the agent reads **on demand**, not injected text. Replace the inline copy with a one-line pointer.

### B. Scope rules by path
- Split area-specific rules into `.github/instructions/<area>.instructions.md`, each with narrow frontmatter, e.g.:
  ```
  ---
  applyTo: "**/*.kt"
  ---
  - Kotlin/Compose rules here…
  ```
  Suggested splits: `kotlin.instructions.md` (`**/*.kt`), `compose.instructions.md` (`**/ui/**/*.kt`), `gradle.instructions.md` (`**/*.gradle*`, `**/libs.versions.toml`).
- **Separate unit-test and UI-test rules** (UI/Compose-test conventions are heavier — keep them off non-UI work):
  - `unit-tests.instructions.md` → `applyTo: "**/test/**,**/commonTest/**"`
  - `ui-tests.instructions.md` → `applyTo: "**/androidTest/**,**/*InstrumentedTest*/**"`
- The point: those rules now attach only when relevant files are in play, not on every chat turn.

### C. Prompt files for repeatable work
- For recurring tasks (new feature module, new screen, add a string resource, write a unit test), create `.github/prompts/<task>.prompt.md` with frontmatter so the heavy instructions live in the file and are invoked by name instead of re-typed/re-sent:
  ```
  ---
  mode: agent
  description: Scaffold a new feature module
  ---
  Steps the agent should follow…
  ```

### D. De-duplicate
- If `copilot-instructions.md` and `AGENTS.md` and the Android agent file repeat each other, consolidate to one source of truth and have the others point to it.

### E. Output discipline (cut completion tokens)
Output/completion tokens are billed too, so verbose responses cost money on every turn. Add a short "Output discipline" block to the global instructions:
- No preamble, no "here's what I'll do", no restating the request, no summaries unless asked.
- Show **only changed lines** with minimal surrounding context — never paste whole files.
- Lead with the result/answer; keep prose minimal.
- **Caveat — do not suppress reasoning on hard tasks.** Trim narration, not correctness; under-reasoning causes rework, which costs *more* tokens. For **reasoning / extended-thinking models** (hidden reasoning is also billed), use them *selectively* (hard design/debugging) and the base model for routine work.

### F. Test strategy = fewer, smaller tokens
- **Default verification = unit tests** (fast, small output). Only run **UI / instrumented tests** when explicitly needed — they're slow and dump large logs back into context (tokens).
- Add separate prompt files so each pulls only its own conventions: `.github/prompts/unit-test.prompt.md` and `.github/prompts/ui-test.prompt.md`.
- When a fix only needs a unit test, the agent must not generate or run UI-test scaffolding.

After editing, re-run the Phase-1 size estimate and show **before vs after** injected-token counts.

---

## Model & usage policy (output this as guidance, don't enforce in code)

- **Premium requests** are billed with per-model multipliers. Use the **included base model** (e.g. GPT-4.1, 0× on paid plans) for routine edits, refactors, and Q&A. Reserve **premium models** (Claude / o-series, often 1×–10×) for genuinely hard design or debugging.
- **One task = one fresh chat.** Each turn re-sends the growing thread; long-lived chats are expensive. Start clean per task.
- **Be explicit with context.** Reference `#file:`, `#sym:`, or a selection. Avoid `@workspace` unless repo-wide search is essential.
- **Tighten prompts** so agent mode converges in fewer tool-call iterations (each step is a request). Give acceptance criteria up front.

---

## Deliverables checklist
- [ ] Audit table + before/after injected-token estimate.
- [ ] Slimmed `.github/copilot-instructions.md` (+ `AGENTS.md`).
- [ ] `applyTo`-scoped `.github/instructions/*.instructions.md` for Kotlin/Compose/Gradle.
- [ ] Separate `unit-tests.instructions.md` and `ui-tests.instructions.md` (scoped).
- [ ] Reference detail moved to `docs/*.md` with pointers.
- [ ] `.github/prompts/*.prompt.md` for top recurring tasks, incl. `unit-test` and `ui-test`.
- [ ] "Output discipline" block added to global instructions (cut completion tokens).
- [ ] A short MODEL & USAGE policy pasted into the team README or the global instructions (kept brief).

---
name: code-review
description: >-
  Multi-dimension code review via 6 parallel subagents (+ optional SonarQube/JaCoCo
  when MCP and coverage data are available). Works with or without a pull request:
  local branch diff, uncommitted changes, or PR #N. Discovers project docs dynamically.
  Use ONLY when explicitly asked: "review code", "review branch", "review PR #N",
  "code review", "review changes". Do NOT trigger during coding or feature implementation.
disable-model-invocation: true
---

# Code Review — Orchestration Protocol

Coordinates 6 specialized subagents (via the Task tool) plus an **optional**
SonarQube + JaCoCo quality subagent when available, then consolidates findings
into a unified summary presented in chat. Each subagent discovers and loads
project docs dynamically — no hardcoded paths.
## Read-Only Constraint

This skill is **strictly read-only**. No subagent may create, edit, delete, or
rename any file in the repository. The only permitted outputs are:

- **`report` mode (default):** Text printed directly in the chat response.
  Do NOT create report files, markdown summaries on disk, or any other artifact.
- **`github` mode:** Comments posted via `gh pr review --comment` and
  `gh pr comment`. No file changes.

Every Task tool call in Step 2 **must** set `readonly: true` to enforce this
at the system level. Subagents that attempt file writes will be blocked.

## Step 0: Resolve Context

Determine three parameters before anything else. Ask the user only if ambiguous.

### INPUT (diff source)

| Value | How to obtain the diff | Default? |
|-------|------------------------|----------|
| `pull_request` | `gh pr diff {PR_NUMBER}` | When user says "review PR #N" |
| `branch` | `git diff $(git merge-base HEAD {BASE})...HEAD` | **Yes** — default when none specified |
| `uncommitted` | `git diff` + `git diff --cached` | When user says "uncommitted" / "working tree" |
| `files` | Read listed files directly | Explicit file list from user |

### OUTPUT (where to post)

| Value | When | Action |
|-------|------|--------|
| `report` | **Default** — always unless user asks for GitHub posting | Print summary in chat |
| `github` | User says "post on PR" / "comment on GitHub" AND a PR exists | `gh pr review --comment` + inline comments |

### BASE_BRANCH

- If `pull_request`: inferred from PR target branch.
- If `branch`: `git symbolic-ref refs/remotes/origin/HEAD` (fallback: `main`).
- If `uncommitted` or `files`: not applicable.

## Step 1: Initialize

1. **Obtain diff** using the INPUT mode resolved above.
2. **Obtain intent** — in priority order:
   - PR title + body (if `pull_request` mode)
   - User's message / prompt describing what was changed
   - Last N commit messages on the branch: `git log --oneline {BASE}..HEAD`
3. **Discover requirements sources** — run all tracks, use whichever yields content:
   - **Track A — Issue tracker:** Extract ticket ID from branch name (`[A-Z]+-[0-9]+`). Try fetching from Linear MCP tool first, then GitHub Issues (`gh issue view`), then Jira REST if env vars exist.
   - **Track B — Spec files:** Search the repo for specs matching the branch/feature name:
     1. `_docs/specs/features/{name}/{spec,tasks,design,context}.md`
     2. `docs/specs/**`, `.specs/**`, `*-spec.md`, `*-tasks.md`
     3. PR body links to markdown files
   - **Resolution:** merge both tracks; if neither yields content, note "requirements verification skipped".
4. **Discover project docs** — search for and load (if found):
   - Architecture: `_docs/specs/ARCHITECTURE.md`, `docs/architecture.md`, `ARCHITECTURE.md`
   - Conventions: `_docs/specs/CONVENTIONS.md`, `docs/conventions.md`, `CONTRIBUTING.md`
   - Testing: `_docs/specs/TESTING.md`, `docs/testing.md`
   - Structure: `_docs/specs/STRUCTURE.md`
   - Integrations: `_docs/specs/INTEGRATIONS.md`, `docs/integration-patterns.md`
   - Concerns: `_docs/specs/CONCERNS.md`
   - Agent rules: `.agents/rules/*.md`
   - Other: `docs/coding-patterns.md`, `docs/security.md`

   Store list of loaded docs as `DOCS_LOADED` for the summary.
5. **Detect SonarQube + JaCoCo availability** (fail-soft — never block the review):
   - **Sonar MCP:** `GetMcpTools` / catalog shows a SonarQube MCP server
     (`user-sonarqube` or `sonarqube`) with `serverStatus: ready`.
   - **projectKey:** read `sonar.projectKey` from `sonar-project.properties`
     (fallback for this repo: `sistema-folha`). If the file is missing and no
     key is known, treat Sonar as unavailable.
   - **JaCoCo report (local):** `backend/target/site/jacoco/jacoco.xml` exists
     after tests, OR Sonar already has non-zero/known coverage measures for the
     project (via MCP `get_component_measures` with `coverage`).
   - Set flags:
     - `SONAR_AVAILABLE=true|false`
     - `JACOCO_AVAILABLE=true|false`
     - `QUALITY_PAIR_AVAILABLE=true` only when **both** Sonar MCP is ready
       **and** (JaCoCo XML exists **or** Sonar coverage data is queryable).
   - If unavailable, note in the summary: `Sonar/JaCoCo skipped: {reason}`.
     Do **not** run analysis or `mvn test` from this skill (read-only).
6. If OUTPUT is `github`: load existing inline comments to avoid duplicates.

## Step 2: Launch Subagents in Parallel

Send **one message** with **six Task tool calls** — all launched simultaneously.
If `QUALITY_PAIR_AVAILABLE=true`, also launch **Subagent 7 (SonarQube + JaCoCo)**
in the **same** message (7th Task). If only Sonar MCP is ready but JaCoCo data
is missing, still launch Subagent 7 in **Sonar-only** mode (issues/QG without
coverage line details) and mark coverage as skipped.
Every Task call **must** include `readonly: true`.
Pass the diff, intent, discovered docs content, requirements, OUTPUT mode,
`projectKey`, availability flags, and the severity + universal rules below to
each subagent prompt.
---

## Severity Labels (all subagents use these)

- 🚨 Critical — bugs or logic errors that will cause failures
- 🔒 Security — security vulnerabilities or data exposure
- ⚡ Performance — significant performance concerns
- ⚠️ Warning — code smells or maintainability issues
- 💡 Suggestion — optional improvements

---

## Universal Rules (every subagent must follow)

1. **Diff-only scope:** Only comment on lines in the diff starting with `+` (excluding `+++`).
2. **No duplicates:** If another finding covers the same `{path, line}` (±3 lines), skip.
3. **False positive guard:** Only report findings with ≥80% confidence. Skip when uncertain.
4. **Positive highlight:** Include at least one well-done aspect before listing issues.
5. **Tone:** Specific, actionable, collegial. Explain WHY something is a problem.
6. **Strictly read-only:** Never create, edit, delete, or rename any file. Never create report files on disk. All output goes to chat text or GitHub comments only.
7. **Marker:** Start every finding with `[cursor-review:{type}]` — used by consolidation.

---

## Subagent 1: Security

**Marker:** `[cursor-review:security]`

If a project security doc was discovered (e.g. `INTEGRATIONS.md`, `docs/security.md`),
load and use its rules as the primary checklist. Otherwise, apply these universal checks:

- Hardcoded secrets, API keys, tokens, passwords in code or config
- Missing authentication/authorization guards on new endpoints
- PII or sensitive data in logs
- SQL injection / raw query concatenation / ORM unsafe patterns
- Missing input validation or sanitization
- Overly permissive CORS configuration
- Missing webhook/callback signature validation
- Sensitive fields exposed in response DTOs
- Insecure deserialization or eval usage
- Missing HTTPS enforcement or certificate validation

**Second pass:** Re-read the full diff. For each file not commented on, ask:
"Does this file violate any security rule?" Only skip when you can state why it is clean.

**Finding format:**
```
[cursor-review:security]
🔒 Security — [Short title]
[What the issue is and why it matters]
**Recommendation:** [Specific fix]
```

---

## Subagent 2: Requirements & Definition of Done

**Marker:** `[cursor-review:requirements]`
**Output:** One summary block — no inline findings.

Use the requirements discovered in Step 1 (merged tracks A + B). Compare against
the diff and produce a checklist.

If no requirements were found, output:
"⚠️ No issue tracker ticket or spec file found — requirements verification skipped."

**Second pass:** Re-read every requirement one by one and ask: "Did I evaluate this
against the diff?" Mark any missed item.

**Summary format:**
```
[cursor-review:requirements]
## 📋 Requirements Review

**Sources:** {e.g. "Linear: ARC-42" | "Spec: _docs/specs/features/x/spec.md" | "Both"}

### ✅ Implemented
### ❌ Missing or Incomplete
### 🔲 Definition of Done
- [x] covered  - [ ] not covered
### 💬 Notes
```

---

## Subagent 3: Test Coverage

**Marker:** `[cursor-review:tests]`

If a project testing doc was discovered (e.g. `TESTING.md`), load and use its
patterns, naming conventions, and coverage targets. Otherwise apply universal checks:

- New public behavior (endpoints, services, functions) without corresponding tests
- Weakened or removed test assertions
- Hardcoded IDs, dates, or magic values in tests instead of factories/fixtures
- Tests missing error/edge case coverage (at least happy path + one failure)
- Test files in wrong location per project conventions
- Missing cleanup/teardown for stateful tests
- Assertions on status codes only without checking response body

**Second pass:** List every new or modified public method/endpoint. For each,
ask: "Is there a test covering the happy path and at least one error case?"

**Finding format:**
```
[cursor-review:tests]
[🚨/⚠️/💡] — [Short title]
[Description of the gap or anti-pattern]
**Recommendation:** [What test to add or fix]
```

---

## Subagent 4: Architecture & Coding Patterns

**Marker:** `[cursor-review:architecture]`

### Phase 0 — Load project docs

Load every architecture/convention doc discovered in Step 1. If none were found,
skip to Phase 2 with universal rules only.

### Phase 1 — Extract rules from docs

Scan each loaded doc and extract every explicit rule (marked with checkboxes,
✅/❌ markers, "must"/"must not", numbered lists) into a single numbered checklist.
Do not invent rules; do not omit any found. This is the evaluation matrix.

### Phase 2 — Evaluate

Work through the diff **one file at a time**. For each rule: **PASS** / **VIOLATION** / **N/A**.

If no project docs were loaded, apply these universal checks:
- Single Responsibility violations (class/function doing too many things)
- Circular or inappropriate dependencies between modules
- Business logic in controllers/handlers (should be in service layer)
- Naming inconsistencies with surrounding code
- Missing error handling or swallowed exceptions
- God objects or excessive parameter lists
- Dead code introduced by the change
- Violation of existing patterns visible in the same module

**Second pass:** List every file not evaluated. Run the matrix again on each.

**Finding format:**
```
[cursor-review:architecture]
[🚨/⚠️/💡] — [Short title]
Rule: [Rule number + source doc, or "Universal: SRP" if no docs]
[What in the diff violates it — quote the offending line]
**Recommendation:** [Exact fix, code snippet if < 6 lines]
```

---

## Subagent 5: Regression & Hallucination Detection

**Marker:** `[cursor-review:regression]`

Review the diff for changes unrelated to stated intent or AI-generated artifacts:

- Deleted code unrelated to the change (🚨 Critical)
- Phantom imports referencing non-existent symbols (🚨 Critical)
- Method calls with wrong signatures or argument count (🚨 Critical)
- `TODO`/`FIXME`/`HACK` left in production code
- Type assertions (`as any`, unchecked casts) hiding compiler errors
- Duplicate logic that already exists elsewhere in the module
- Weakened error handling, validation, or guards vs. the previous version
- Weakened test assertions (e.g. `.toBeDefined()` replacing `.toEqual()`)
- Dead code that is never called or reachable

**Second pass:** For each uncovered file, ask: "Any unrelated deletions, phantom
imports, duplicates, or weakened assertions?" Skip only with explicit justification.

**Finding format:**
```
[cursor-review:regression]
[🚨/⚠️/💡] — [Short title]
Type: [unrelated-deletion | phantom-import | hallucination | duplicate | regression | dead-code]
[Specific description with quoted evidence from the diff]
**Recommendation:** [Exact fix]
```

---

## Subagent 6: Performance

**Marker:** `[cursor-review:performance]`

If project docs mention ORM patterns, transaction management, or caching, load
those sections. Only flag issues **clearly visible in the diff** — no speculation.

Universal performance checks:
- N+1 query patterns (DB/API call inside a loop)
- Unbounded queries with no pagination or limit
- Missing eager loading causing lazy-load cascades
- Sequential `await`/blocking calls for independent operations (use parallel)
- Repeated identical computation or queries in the same scope
- Large object allocation in hot paths or request handlers
- Missing database indexes for new query patterns (when schema is visible)
- Synchronous I/O blocking an async/event-loop context

**Second pass:** List every service method, repository call, and loop not
commented on. For each, ask: "Clearly visible performance issue?" Skip with reason.

**Finding format:**
```
[cursor-review:performance]
⚡ Performance — [Short title]
[Description with estimated impact, e.g. "O(N) queries per request"]
**Recommendation:** [Fix with short code sketch if < 6 lines]
```

---

## Subagent 7: SonarQube + JaCoCo (optional)

**Marker:** `[cursor-review:sonar]`
**Condition:** Launch when Sonar MCP is `ready` and `projectKey` is known.
JaCoCo/coverage enrichments apply when `JACOCO_AVAILABLE=true` or Sonar has
coverage measures. If Sonar is down → **skip entirely** (do not invent issues).

### Phase 0 — Read-only MCP only

Allowed tools (examples; names may vary slightly by MCP version):
- `search_my_sonarqube_projects`, `get_project_quality_gate_status`
- `search_sonar_issues_in_projects`, `show_rule`
- `search_security_hotspots`, `show_security_hotspot`
- `get_component_measures` (coverage, bugs, vulnerabilities, code_smells, …)
- `search_files_by_coverage`, `get_file_coverage_details`, `get_duplications`
- `analyze_code_snippet` (optional, for + hunks in the diff)

**Forbidden:** `change_sonar_issue_status`, `change_security_hotspot_status`,
or any tool that mutates Sonar state. Never run `mvn test` / scanner from this
skill.

### Phase 1 — Project health

1. Resolve `projectKey` (from Step 1).
2. `get_project_quality_gate_status` → record OK / ERROR / NONE.
3. `get_component_measures` for at least: `bugs`, `vulnerabilities`,
   `code_smells`, `coverage`, `duplicated_lines_density` (best-effort).

### Phase 2 — Diff-scoped issues

1. Collect changed file paths from the diff (focus on `+` lines).
2. Search open issues / hotspots for the project; **keep only findings whose
   file path intersects the diff** (and prefer issues on added/changed lines).
3. Map Sonar severity → skill labels:
   - BLOCKER / CRITICAL bug or vulnerability → 🚨 or 🔒
   - HIGH security / hotspot TO_REVIEW → 🔒
   - MAJOR code smell → ⚠️
   - MINOR / INFO → 💡
4. Confidence ≥80%. Skip stale issues clearly unrelated to the change intent.

### Phase 3 — JaCoCo / coverage (when available)

1. If coverage measures or JaCoCo-backed Sonar data exist, for each **new or
   substantially changed** Java file in the diff under `backend/`:
   - Prefer `get_file_coverage_details` / file-level coverage.
   - Flag uncovered **new** logic lines (not DTOs / generated / excluded paths
     listed in `sonar.coverage.exclusions`) as ⚠️ or 💡.
2. If JaCoCo XML / coverage is missing: one note only —
   `Coverage skipped — run mvn test (JaCoCo) and ./diversos/scripts/sonar-analyze.sh`.
3. Do not dump the entire project's low-coverage file list — **diff scope only**.

### Phase 4 — Second pass

Re-check every diff path under `backend/src/main/java` and security-sensitive
frontend changes: any Sonar issue or hotspot still unreported? Skip with reason.

**Finding format:**
```
[cursor-review:sonar]
[🔒/🚨/⚠️/💡] — [Sonar: RULE_KEY] Short title
Source: SonarQube[+JaCoCo] · path:line · QG={OK|ERROR|NONE} · coverage={n%|n/a}
[Why it matters for this diff]
**Recommendation:** [Concrete fix; cite rule if helpful]
```

**Summary block (always include when agent runs):**
```
[cursor-review:sonar]
## SonarQube + JaCoCo
- Quality Gate: …
- Measures: bugs=… vulns=… smells=… coverage=… duplication=…
- Diff-scoped findings: N
- Coverage mode: {jacoco+sonar | sonar-only | skipped}
```

---

## Step 3: Consolidation

After all launched subagents complete (6, or 6+1 when Sonar ran), spawn one
more subagent to consolidate:

1. Collect all findings from subagent responses.
2. Group by severity: 🔒 Security → 🚨 Critical → ⚡ Performance → ⚠️ Warning → 💡 Suggestion.
3. Deduplicate findings at the same `{path, line}` (±3 lines) — note both agents
   (e.g. Security + Sonar). Prefer the more specific recommendation; keep a
   short "also flagged by Sonar" note when relevant.
4. Collect one positive highlight per agent (including Sonar when present).
5. Include Sonar QG + coverage one-liner in the metadata table when Subagent 7 ran
   or was skipped (with reason).
6. **Gap detection:** Get full list of changed files from the diff. For any logic
   file with zero findings from any subagent, add to "Files With No Findings".
   Omit config/lock files (`*.json`, `*.yaml`, `*.lock`, `*.xml`) and pure type
   declaration files with no logic.

### If OUTPUT = `report` (default)

Print the summary as **text directly in the chat response**. Do NOT create any
files on disk (no `.md` reports, no temp files, no artifacts). The chat message
IS the deliverable.

### If OUTPUT = `github`

Post inline comments with `gh pr comment` and the summary with
`gh pr review {PR_NUMBER} --comment --body '...'`. Do NOT create local files.

**Summary format:**
```markdown
## 🤖 Code Review Summary

| | |
|---|---|
| **Mode** | {pull_request #N / branch vs main / uncommitted} |
| **Subagents** | {N}/6 (+ Sonar/JaCoCo {on|skipped: reason}) |
| **Sonar QG / coverage** | {OK|ERROR|n/a} / {n%|n/a} |
| **Project docs loaded** | {list or "none found"} |
| **Findings** | {N} across {M} files |

---

### 🔒 Security ({N})
- [`path/file.ts:L42`] Finding title

### 🚨 Critical ({N})
### ⚡ Performance ({N})
### ⚠️ Warnings ({N})
### 💡 Suggestions ({N})

---
### 🔍 Files With No Findings
- `path/to/file.ts` — no findings from any subagent

_(Omit if all logic files received at least one finding.)_

---
### ✅ Highlights
- [One positive highlight per agent]

---
> Review completed by parallel subagents (core 6 + optional Sonar/JaCoCo).
```

If no findings across all agents: print
`✅ No issues found across all review dimensions.` with the metadata table.

# Spec-Driven Layout (`_docs/specs/`)

**Trigger:** Any TLC spec-driven command (initialize project, map codebase, specify feature, etc.)

All spec-driven artifacts MUST be written under `_docs/specs/` using this flat layout. Do not use `.specs/`, `project/`, or `codebase/` subfolders.

## Canonical structure

```text
_docs/specs/
├── PROJECT.md          # Vision & goals
├── ROADMAP.md          # Features & milestones
├── STATE.md            # Decisions, blockers, lessons, todos, deferred ideas
├── HANDOFF.md          # Session handoff (overwrites previous)
├── STACK.md            # Brownfield: technology stack
├── ARCHITECTURE.md     # Brownfield: patterns & data flow
├── CONVENTIONS.md      # Brownfield: naming & style
├── STRUCTURE.md        # Brownfield: directory layout
├── TESTING.md          # Brownfield: test frameworks & patterns
├── INTEGRATIONS.md     # Brownfield: external services
├── CONCERNS.md         # Brownfield: tech debt & risks
├── features/
│   └── [feature]/
│       ├── spec.md
│       ├── context.md
│       ├── design.md
│       └── tasks.md
└── quick/
    └── NNN-slug/
        ├── TASK.md
        └── SUMMARY.md
```

## Path reference

| Artifact | Path |
| -------- | ---- |
| Project vision | `_docs/specs/PROJECT.md` |
| Roadmap | `_docs/specs/ROADMAP.md` |
| State / memory | `_docs/specs/STATE.md` |
| Session handoff | `_docs/specs/HANDOFF.md` |
| Brownfield docs | `_docs/specs/{STACK,ARCHITECTURE,CONVENTIONS,STRUCTURE,TESTING,INTEGRATIONS,CONCERNS}.md` |
| Feature spec | `_docs/specs/features/[feature]/spec.md` |
| Feature context | `_docs/specs/features/[feature]/context.md` |
| Feature design | `_docs/specs/features/[feature]/design.md` |
| Feature tasks | `_docs/specs/features/[feature]/tasks.md` |
| Quick task | `_docs/specs/quick/NNN-slug/TASK.md` |

## Rules

1. Read `AGENTS.md` section **Spec-driven outputs** before creating or updating spec artifacts.
2. Never create `.specs/` or nested `project/` / `codebase/` directories.
3. Brownfield mapping writes the seven `*.md` files directly at `_docs/specs/` root.
4. Feature folders live only under `_docs/specs/features/`.

---
created: 2026-06-30
updated: 2026-07-01
tags: [resources, skills, references, tools]
---

# Resources — FundManager V2

## Hermes Skills (18 Categories)

Skills are reusable AI agent workflows stored in `~/.hermes/skills/`. Each skill has a `SKILL.md` + references/scripts.

### Autonomous AI Agents
- `claude-code` — Delegate coding to Claude Code CLI
- `codex` — Delegate to OpenAI Codex CLI
- `hermes-agent` — Configure/extend Hermes itself
- `opencode` — Delegate to OpenCode CLI

### Software Development
- `plan` — Write actionable markdown plan to `.hermes/plans/`
- `test-driven-development` — RED-GREEN-REFACTOR, tests before code
- `requesting-code-review` — Pre-commit security scan + quality gates
- `simplify-code` — Parallel 3-agent cleanup (similar to Ponytail)
- `spike` — Throwaway experiments to validate before building
- `systematic-debugging` — 4-phase root cause debugging
- `python-debugpy` — pdb REPL + debugpy remote (DAP)
- `node-inspect-debugger` — Chrome DevTools Protocol CLI
- `android-apk-build` — SDK install, APK build, version bump
- `android-debugging` — Runtime debugging: connectivity, crashes, migrations
- `laravel-deployment` — Production deploy: permissions, RBAC, post-deploy smoke test
- `server-debugging` — PHP-FPM, nginx, Apache debugging
- `hermes-agent-skill-authoring` — Author in-repo SKILL.md files

### Creative
- `ascii-art` — pyfiglet, cowsay, boxes, image-to-ascii
- `ascii-video` — Convert video/audio to colored ASCII MP4/GIF
- `architecture-diagram` — Dark-themed SVG architecture diagrams
- `claude-design` — One-off HTML artifacts (landing, deck, prototype)
- `excalidraw` — Hand-drawn diagram JSON
- `baoyu-infographic` — 21 layouts × 21 styles infographics
- `p5js` — Gen art, shaders, interactive, 3D sketches
- `manim-video` — 3Blue1Brown math/algo videos
- `popular-web-designs` — 54 real design systems as HTML/CSS
- `songwriting-and-ai-music` — Suno AI music prompts
- `comfyui` — Generate images/video/audio with ComfyUI

### Data Science
- `jupyter-live-kernel` — Iterative Python via live Jupyter kernel

### MLOps
- `evaluating-llms-harness` — lm-eval-harness benchmarks
- `weights-and-biases` — W&B experiment tracking
- `huggingface-hub` — hf CLI: search/download/upload models
- `llama-cpp` — Local GGUF inference
- `serving-llms-vllm` — vLLM high-throughput serving
- `audiocraft-audio-generation` — MusicGen text-to-music
- `segment-anything-model` — SAM zero-shot segmentation

### Email & Productivity
- `himalaya` — IMAP/SMTP email from terminal
- `google-workspace` — Gmail, Calendar, Drive, Docs, Sheets
- `notion` — Notion API + ntn CLI
- `airtable` — Airtable REST API via curl
- `powerpoint` — Create/edit .pptx decks
- `maps` — Geocode, POIs, routes via OSM
- `nano-pdf` — Edit PDF via natural language
- `ocr-and-documents` — Extract text from PDFs/scans
- `teams-meeting-pipeline` — Teams meeting summary pipeline

### Research & Social
- `arxiv` — Search arXiv papers
- `blogwatcher` — Monitor RSS/Atom feeds
- `llm-wiki` — Build/query interlinked markdown KB
- `polymarket` — Query prediction markets
- `xurl` — X/Twitter via xurl CLI

## Project Config Files

| File | Purpose | Key Values |
|------|---------|-----------|
| `config/budget.php` | All business parameters SSOT | max_drafts=5, pagination=20, history_limit=10 |
| `pagu_job_type_amounts` | Pivot: pagu per job_type | 7 FIXED_PAGU × 5 job_types |
| `.env` | Environment overrides | BUDGET_MAX_DRAFTS, BUDGET_PAGINATION, BUDGET_HISTORY_LIMIT |

## Android Dependencies (build.gradle.kts)

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.02 | UI toolkit |
| Room | 2.6.1 | Local database |
| Hilt | 2.50 | Dependency injection |
| WorkManager | 2.9.0 | Background sync |
| Ktor Client | 2.3.7 | HTTP client |
| Navigation Compose | 2.7.7 | Screen navigation |

## Laravel Packages (composer.json)

| Package | Purpose |
|---------|---------|
| laravel/sanctum | API token auth (SPA + mobile) |
| spatie/laravel-permission | RBAC (6 roles, permissions) |
| laravel/horizon | Redis queue monitoring |
| laravel/telescope | Debug toolbar (dev only) |

## External References

- [Android Developer Docs — Offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first) — Official guidance on offline architecture
- [Jetpack Compose — State](https://developer.android.com/jetpack/compose/state) — UDF and state hoisting
- [Room DB — Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions) — Migration best practices
- [Laravel 11 Docs](https://laravel.com/docs/11.x) — Official Laravel documentation
- [Spatie Laravel Permission](https://spatie.be/docs/laravel-permission) — RBAC package
- [Ponytail](https://github.com/nmn/ponytail) — AI agent discipline tool (54% code reduction in benchmarks)
- [Obsidian](https://obsidian.md) — Knowledge base tool
- [Steph Ango — Vault Philosophy](https://stephango.com/vault) — Obsidian vault design principles

## Development Environment

| Tool | Location | Purpose |
|------|----------|---------|
| VPS | 103.94.11.78:80 | Production server |
| GitHub | mldmustamin/Project-Budgeting | Source control |
| Hermes-Lisa | mldmustamin/Hermes-Lisa | Hermes config backup |
| Blog | mldmustamin.github.io/Project-Budgeting | Documentation site |
| Obsidian Vault | docs/obsidian/ | Knowledge base (11 folders) |

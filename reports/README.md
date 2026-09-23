# Reports — generated artifacts

> Output of Loop A (architecture maintenance). Everything here is **generated**
> by the agent via `GENERATE_REPORT`, the nightly pipeline, or a skill run.
> Nothing here is hand-written, and nothing here is a source of truth.

```
reports/
├── daily/          DAILY project-state snapshots
├── weekly/         WEEKLY digests
├── architecture/   Architecture Health Reports
├── drift/          Code ↔ model drift, one per feature or per scan
├── tech-debt/      TECH_DEBT_REPORT runs
└── release/        RELEASE_READINESS_REPORT runs
```

Naming: `YYYY-MM-DD[-<scope>].md`. Reports are append-only history — regenerate
into a new file, never overwrite an old one, or the trend line disappears.

Reports may be deleted wholesale; they are always reproducible from the digital
twin. That is the test of whether something belongs here rather than in
`project-memory/` or an ADR.

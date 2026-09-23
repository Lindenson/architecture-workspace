# jQAssistant — scan rules and reports

> Inputs to Loop A's structural layer (L1). `jqassistant-mcp` reads the resulting
> graph from Neo4j; this directory holds the rules that shape it.

```
jqassistant/
├── rules/          Concept and constraint definitions (.xml / .adoc)
├── reports/        Scan output, checked in only when it is evidence for a finding
└── jqassistant.yml Scan configuration (see config/jqassistant.config.example.yml)
```

## Scanning a product repository

```bash
# from the product repo, after a build
jqassistant scan -f target/classes
jqassistant analyze -rule-directory <workspace>/jqassistant/rules
jqassistant server        # or point Neo4j at the store and let jqassistant-mcp query it
```

## Graph schema the MCP tools assume

Nodes `:Type` `:Package` `:Artifact` · edges `:DEPENDS_ON` · fully-qualified name
on `fqn` (queries use `coalesce(n.fqn, n.name)` defensively). If your scan
produces a different shape, the tools return empty results rather than failing —
check here first when a blast radius comes back suspiciously small.

## Rules belong here, not in code

A constraint expressed in `architecture/constraints/` should have a machine
counterpart: either an ArchUnit test in `architecture-tests/` or a jQAssistant
constraint here. A rule with neither is documentation, not governance.

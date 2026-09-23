# Plugin packaging notes

`plugin.json` points `skills`, `agents`, `hooks` and `mcpServers` at paths under
`.claude/` so the workspace works both as a repository you open directly and as
a plugin installed elsewhere.

**Verify this before relying on it.** Plugin component discovery conventions
differ between Claude Code versions — some expect `skills/`, `agents/`,
`hooks/hooks.json` at the plugin root rather than explicit path fields. After
installing:

```bash
/plugin install architecture-intelligence@aip-marketplace
/reload-plugins
claude --debug hooks     # are the hooks registered?
```

Then ask for something that should route to `spec-critic`. If the skill does not
fire, the description usually needs sharpening — that is the most common cause.
If *nothing* from the plugin loads, the path fields are the suspect: move the
directories to the plugin root and drop the explicit fields.

Hooks inside the plugin use `${CLAUDE_PLUGIN_ROOT}`; the in-repo `settings.json`
uses `$CLAUDE_PROJECT_DIR`. Neither hardcodes a developer's home directory.

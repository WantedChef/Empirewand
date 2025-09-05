# Codex Plus (Companion Extension)

Codex Plus enhances Codex with a rich UI: profiles, model picker (incl. legacy + custom), and easy runtime toggles synced to `codex.config.json`.

## Features
- Activity Bar: Settings (webview), History, Logs
- Profiles: Low/Medium/High/Creative/Strict Code with presets (temperature, reasoning, tokens)
- Models: dropdown (modern + optional legacy + custom), custom endpoint
- Runtime: auto-approve, sandbox mode, network access
- Commands: Open Settings, Select Profile, Select Model, Toggle Auto-Approve

## Install (Local)
- Open this folder in VS Code and press F5 (Extension Development Host), or package and install:
  - npm run package
  - npm run install:vsix

## Settings (workspace)
- `codex.profile`, `codex.model`
- `codex.temperature`, `codex.reasoningBudget`, `codex.maxTokens`
- `codex.legacyModels`, `codex.customModels`, `codex.modelEndpoint`
- `codex.autoApprove`, `codex.sandboxMode`, `codex.networkAccess`

## Runtime Sync
Writes to root `codex.config.json`:
- `approval_policy` (never | on-request)
- `sandbox_mode` (workspace-write | danger-full-access)
- `network_access` (enabled | restricted)
- `auto_apply_patches` (boolean)


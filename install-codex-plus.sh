#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/tools/codex-plus"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js is required to package the extension. Install Node 18+." >&2
  exit 1
fi

npm run package

if command -v code >/dev/null 2>&1; then
  npm run install:vsix
else
  echo "VS Code CLI (code) not found. Install manually from dist/." >&2
fi


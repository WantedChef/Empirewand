const { execSync } = require('child_process');
const { existsSync, mkdirSync } = require('fs');
const { join } = require('path');

function run(cmd) {
  execSync(cmd, { stdio: 'inherit', shell: true });
}

(function main() {
  const distDir = join(__dirname, '..', 'dist');
  if (!existsSync(distDir)) mkdirSync(distDir);
  // Prefer npx to avoid global install requirements
  run('npx --yes @vscode/vsce package --no-yarn --out dist/codex-plus.vsix');
})();


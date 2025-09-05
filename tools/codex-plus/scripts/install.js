const { execSync } = require('child_process');
const { existsSync, readdirSync } = require('fs');
const { join } = require('path');

function run(cmd) { execSync(cmd, { stdio: 'inherit', shell: true }); }

(function main() {
  const distDir = join(__dirname, '..', 'dist');
  if (!existsSync(distDir)) {
    console.error('No dist/ directory. Run: npm run package');
    process.exit(1);
  }
  const files = readdirSync(distDir).filter(f => f.endsWith('.vsix')).sort();
  if (!files.length) {
    console.error('No .vsix found in dist/. Run: npm run package');
    process.exit(1);
  }
  const vsix = join(distDir, files[files.length - 1]);
  run(`code --install-extension "${vsix}"`);
})();


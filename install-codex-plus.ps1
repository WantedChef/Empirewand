Param(
  [switch]$PackageOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Push-Location "tools/codex-plus"
try {
  if (!(Test-Path package.json)) { throw 'Not in codex-plus folder' }
  if (Get-Command node -ErrorAction SilentlyContinue) {
    npm run package
    if (-not $PackageOnly) {
      npm run install:vsix
    }
  } else {
    Write-Error 'Node.js is required to package the extension. Install Node 18+.'
  }
} finally {
  Pop-Location
}


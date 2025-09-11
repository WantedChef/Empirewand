param(
    [string]$Path = "src/main/java/nl/wantedchef/empirewand/spell",
    [switch]$WhatIf
)

Write-Host "Starting brace fix for executeSpell methods..." -ForegroundColor Green

# Get all Java files in the implementation directory
$javaFiles = Get-ChildItem -Path $Path -Recurse -Filter "*.java" | Where-Object {
    $_.Name -notmatch "(Aura|EmpireAura|Confuse|Comet)\.java$"
}

Write-Host "Found $($javaFiles.Count) files to fix" -ForegroundColor Yellow

foreach ($file in $javaFiles) {
    Write-Host "Processing: $($file.FullName)" -ForegroundColor Cyan

    $content = Get-Content $file.FullName -Raw
    $originalContent = $content

    # Fix: Add missing opening brace after executeSpell method signature
    $content = $content -replace '(protected Void executeSpell\(SpellContext context\)\s*)\n(\s*)([^{])', @"
`$1 {
`$2`$3
"@

    # Fix: Remove malformed return statements that are in the wrong place
    $content = $content -replace '\s*return;\s*\}\s*return null; // Void effect', '        return null; // Void effect'

    # Only write if content changed
    if ($content -ne $originalContent) {
        if ($WhatIf) {
            Write-Host "  Would fix: $($file.Name)" -ForegroundColor Yellow
        }
        else {
            $content | Set-Content $file.FullName -Encoding UTF8
            Write-Host "  Fixed: $($file.Name)" -ForegroundColor Green
        }
    }
    else {
        Write-Host "  No changes needed: $($file.Name)" -ForegroundColor Gray
    }
}

Write-Host "Brace fix complete!" -ForegroundColor Green
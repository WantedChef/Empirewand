param(
    [string]$Path = "src/main/java/com/example/empirewand/spell/implementation",
    [switch]$WhatIf
)

Write-Host "Starting comprehensive fix for spell implementations..." -ForegroundColor Green

# Get all Java files in the implementation directory
$javaFiles = Get-ChildItem -Path $Path -Recurse -Filter "*.java" | Where-Object {
    $_.Name -notmatch "(Aura|EmpireAura|Confuse|Comet|Polymorph)\.java$"
}

Write-Host "Found $($javaFiles.Count) files to fix" -ForegroundColor Yellow

foreach ($file in $javaFiles) {
    Write-Host "Processing: $($file.FullName)" -ForegroundColor Cyan

    $content = Get-Content $file.FullName -Raw
    $originalContent = $content

    # Fix 1: Remove malformed return statements that appear in wrong places
    $content = $content -replace '\s*return;\s*\}\s*return null; // Void effect', '            return null; // Void effect'

    # Fix 2: Fix executeSpell method structure - add missing opening brace
    if ($content -match 'protected Void executeSpell\(SpellContext context\)\s*[^{]') {
        $content = $content -replace '(protected Void executeSpell\(SpellContext context\)\s*)\n(\s*)([^{])', @"
`$1 {
`$2`$3
"@
    }

    # Fix 3: Fix cases where return null appears in wrong place within if statements
    $content = $content -replace '(\s*)context\.fx\(\)\.fizzle\(player\);\s*return;\s*\}\s*return null; // Void effect', @"
`$1        context.fx().fizzle(player);
`$1        return null; // Void effect
"@

    # Fix 4: Clean up extra closing braces
    $content = $content -replace '\s*\}\s*\}\s*return null; // Void effect', @"

        return null; // Void effect
    }
"@

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

Write-Host "Comprehensive fix complete!" -ForegroundColor Green
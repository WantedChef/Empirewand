# PowerShell script to remove only unused EffectService imports
# This script only removes the EffectService import which was clearly unused

$files = Get-ChildItem -Path "src\main\java\com\example\empirewand\spell" -Recurse -Filter "*.java" -File

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw

    # Only remove unused EffectService import
    $content = $content -replace "(?m)^import com\.example\.empirewand\.api\.EffectService;\s*$", ""

    # Write back the cleaned content
    Set-Content -Path $file.FullName -Value $content -NoNewline
}

Write-Host "EffectService import removal completed!"
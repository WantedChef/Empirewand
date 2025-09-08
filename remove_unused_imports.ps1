# PowerShell script to remove unused imports from Java files
# This script removes common unused imports from spell implementation files

$files = Get-ChildItem -Path "src\main\java\com\example\empirewand\spell" -Recurse -Filter "*.java" -File

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw

    # Remove unused EffectService import
    $content = $content -replace "(?m)^import com\.example\.empirewand\.api\.EffectService;\s*$", ""

    # Remove unused Component import
    $content = $content -replace "(?m)^import net\.kyori\.adventure\.text\.Component;\s*$", ""

    # Remove unused Entity import
    $content = $content -replace "(?m)^import org\.bukkit\.entity\.Entity;\s*$", ""

    # Remove unused List import
    $content = $content -replace "(?m)^import java\.util\.List;\s*$", ""

    # Remove unused Map import
    $content = $content -replace "(?m)^import java\.util\.Map;\s*$", ""

    # Remove unused Queue import
    $content = $content -replace "(?m)^import java\.util\.Queue;\s*$", ""

    # Remove unused ConfigurationSection import
    $content = $content -replace "(?m)^import org\.bukkit\.configuration\.ConfigurationSection;\s*$", ""

    # Remove unused Sound import
    $content = $content -replace "(?m)^import org\.bukkit\.Sound;\s*$", ""

    # Remove unused Vector import
    $content = $content -replace "(?m)^import org\.bukkit\.util\.Vector;\s*$", ""

    # Remove unused BukkitRunnable import
    $content = $content -replace "(?m)^import org\.bukkit\.scheduler\.BukkitRunnable;\s*$", ""

    # Remove unused LivingEntity import
    $content = $content -replace "(?m)^import org\.bukkit\.entity\.LivingEntity;\s*$", ""

    # Remove unused Player import
    $content = $content -replace "(?m)^import org\.bukkit\.entity\.Player;\s*$", ""

    # Remove unused Location import
    $content = $content -replace "(?m)^import org\.bukkit\.Location;\s*$", ""

    # Write back the cleaned content
    Set-Content -Path $file.FullName -Value $content -NoNewline
}

Write-Host "Unused imports removal completed!"
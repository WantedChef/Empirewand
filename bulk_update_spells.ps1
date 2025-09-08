param(
    [string]$Path = "src/main/java/com/example/empirewand/spell/implementation",
    [switch]$WhatIf
)

Write-Host "Starting bulk update of spell implementations..." -ForegroundColor Green

# Get all Java files in the implementation directory
$javaFiles = Get-ChildItem -Path $Path -Recurse -Filter "*.java" | Where-Object {
    $_.Name -notmatch "(Aura|EmpireAura|Confuse|Comet)\.java$"
}

Write-Host "Found $($javaFiles.Count) files to update" -ForegroundColor Yellow

foreach ($file in $javaFiles) {
    Write-Host "Processing: $($file.FullName)" -ForegroundColor Cyan

    $content = Get-Content $file.FullName -Raw

    # Skip if already updated
    if ($content -match "extends Spell<") {
        Write-Host "  Already updated, skipping" -ForegroundColor Gray
        continue
    }

    $originalContent = $content

    # 1. Update imports
    $content = $content -replace 'import com\.example\.empirewand\.spell\.Prereq;', 'import com.example.empirewand.spell.PrereqInterface;'
    $content = $content -replace 'import net\.kyori\.adventure\.text\.Component;', ''

    # 2. Update class declaration
    if ($content -match 'implements ProjectileSpell') {
        # Handle projectile spells
        $className = [regex]::Match($content, 'public class (\w+) implements ProjectileSpell').Groups[1].Value
        $content = $content -replace 'public class \w+ implements ProjectileSpell \{', @"
public class $className extends ProjectileSpell {

    public $className() {
        super(new ProjectileSpell.Builder()
                .name("$className")
                .description("$className spell")
                .manaCost(15)
                .cooldown(java.time.Duration.ofSeconds(20))
                .speed(1.0)
                .isHoming(false)
                .trailParticle(org.bukkit.Particle.CRIT)
                .hitSound(org.bukkit.Sound.ENTITY_GENERIC_EXPLODE)
                .hitVolume(1.0f)
                .hitPitch(1.0f));
    }
"@
    }
    elseif ($content -match 'implements Spell') {
        # Handle regular spells
        $className = [regex]::Match($content, 'public class (\w+) implements Spell').Groups[1].Value
        $content = $content -replace 'public class \w+ implements Spell \{', @"
public class $className extends Spell<Void> {

    public $className() {
        super(new Builder<Void>()
                .name("$className")
                .description("$className spell")
                .manaCost(10)
                .cooldown(java.time.Duration.ofSeconds(30)));
    }
"@
    }

    # 3. Update execute method
    $content = $content -replace 'public void execute\(SpellContext context\)', 'protected Void executeSpell(SpellContext context)'

    # 4. Add return null; if missing
    if ($content -match 'protected Void executeSpell\(SpellContext context\)' -and $content -notmatch 'return null;') {
        $content = $content -replace '\s*\}\s*$', @"

        return null; // Void effect
    }
"@
    }

    # 5. Add handleEffect method after executeSpell
    if ($content -match 'executeSpell' -and $content -notmatch 'handleEffect') {
        $content = $content -replace '(return null; // Void effect\s*\})', @"
        return null; // Void effect
    }

    @Override
    protected void handleEffect(SpellContext context, Void effect) {
        // No additional handling needed for Void effect
    }
"@
    }

    # 6. Update prereq method
    $content = $content -replace 'public Prereq prereq\(\)', 'public PrereqInterface prereq()'
    $content = $content -replace 'return new Prereq\(true, Component\.text\(""\)\);', 'return new PrereqInterface.NonePrereq();'

    # 7. Remove old methods
    $content = $content -replace 'public String getName\(\) \{\s*return "\w+";\s*\}', ''
    $content = $content -replace 'public Component displayName\(\) \{\s*return Component\.text\("[^"]*"\);\s*\}', ''

    # 8. Add missing imports if needed
    if ($content -match 'SpellContext' -and $content -notmatch 'import.*SpellContext') {
        $content = $content -replace '(package .*;)', @"
`$1

import com.example.empirewand.spell.SpellContext;
"@
    }

    if ($content -match 'PrereqInterface' -and $content -notmatch 'import.*PrereqInterface') {
        $content = $content -replace '(package .*;)', @"
`$1

import com.example.empirewand.spell.PrereqInterface;
"@
    }

    # Only write if content changed
    if ($content -ne $originalContent) {
        if ($WhatIf) {
            Write-Host "  Would update: $($file.Name)" -ForegroundColor Yellow
        }
        else {
            $content | Set-Content $file.FullName -Encoding UTF8
            Write-Host "  Updated: $($file.Name)" -ForegroundColor Green
        }
    }
    else {
        Write-Host "  No changes needed: $($file.Name)" -ForegroundColor Gray
    }
}

Write-Host "Bulk update complete!" -ForegroundColor Green
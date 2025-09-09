$basePath = "src\main\java\nl\wantedchef\empirewand"

# Move EmpireWandPlugin
Move-Item -Path "$basePath\EmpireWandPlugin.java" -Destination "$basePath\empirewand\EmpireWandPlugin.java"

# Move visual to common/visual
New-Item -ItemType Directory -Path "$basePath\common\visual"
Move-Item -Path "$basePath\visual\*" -Destination "$basePath\common\visual"
Remove-Item -Path "$basePath\visual" -Recurse

# Move command framework
Move-Item -Path "$basePath\command\framework" -Destination "$basePath\framework\command"

# Move services
Move-Item -Path "$basePath\core\services\*" -Destination "$basePath\framework\service"
Remove-Item -Path "$basePath\core\services" -Recurse

# Move spell implementations
Get-ChildItem -Path "$basePath\spell\implementation" -Directory | ForEach-Object {
    $category = $_.Name
    $newPath = "$basePath\features\spell\$category"
    Get-ChildItem -Path $_.FullName -Filter *.java | ForEach-Object {
        $spellName = $_.BaseName
        $newSpellPath = "$newPath\$spellName"
        New-Item -ItemType Directory -Path $newSpellPath -ErrorAction SilentlyContinue
        Move-Item -Path $_.FullName -Destination $newSpellPath
    }
}
Remove-Item -Path "$basePath\spell\implementation" -Recurse

# Move listeners
Get-ChildItem -Path "$basePath\listeners" -Recurse -Filter *.java | ForEach-Object {
    Move-Item -Path $_.FullName -Destination "$basePath\listener"
}
Remove-Item -Path "$basePath\listeners" -Recurse

# Update package and import statements
Get-ChildItem -Path $basePath -Recurse -Filter *.java | ForEach-Object {
    $content = Get-Content -Path $_.FullName -Raw
    $newContent = $content.Replace("nl.wantedchef.empirewand.visual", "nl.wantedchef.empirewand.common.visual")
    $newContent = $newContent.Replace("nl.wantedchef.empirewand.command.framework", "nl.wantedchef.empirewand.framework.command")
    $newContent = $newContent.Replace("nl.wantedchef.empirewand.core.services", "nl.wantedchef.empirewand.framework.service")
    $newContent = $newContent.Replace("nl.wantedchef.empirewand.spell.implementation", "nl.wantedchef.empirewand.features.spell")
    $newContent = $newContent.Replace("nl.wantedchef.empirewand.listeners", "nl.wantedchef.empirewand.listener")

    # Update package statement
    $relativePath = $_.Directory.FullName.Substring($basePath.Length + 1).Replace("\", ".")
    $newPackage = "package nl.wantedchef.empirewand.$relativePath"
    $newContent = $newContent -replace "package nl.wantedchef.empirewand.*", $newPackage

    Set-Content -Path $_.FullName -Value $newContent
}

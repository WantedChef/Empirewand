$oldPackage = "com.example.empirewand"
$newPackage = "nl.wantedchef.empirewand"
$basePath = "src\main\java"

$oldPath = "$basePath\com\example\empirewand"
$newPath = "$basePath\nl\wantedchef\empirewand"

Get-ChildItem -Path $oldPath -Recurse -Filter *.java | ForEach-Object {
    $oldFilePath = $_.FullName
    $newFilePath = $oldFilePath.Replace($oldPath, $newPath)
    $newFileDir = Split-Path -Path $newFilePath -Parent

    if (-not (Test-Path -Path $newFileDir)) {
        New-Item -ItemType Directory -Path $newFileDir | Out-Null
    }

    $content = Get-Content -Path $oldFilePath -Raw
    $newContent = $content.Replace($oldPackage, $newPackage)

    Set-Content -Path $newFilePath -Value $newContent
    Remove-Item -Path $oldFilePath
}

# Clean up empty directories
$oldExampleDir = "$basePath\com\example"
if (Test-Path -Path $oldExampleDir) {
    Remove-Item -Path $oldExampleDir -Recurse -Force
}

$oldComDir = "$basePath\com"
if (Test-Path -Path $oldComDir) {
    if ((Get-ChildItem -Path $oldComDir).Count -eq 0) {
        Remove-Item -Path $oldComDir -Force
    }
}

# Собирает мод и кладёт готовый jar в build/libs.
# Запуск:  powershell -ExecutionPolicy Bypass -File .\build.ps1
$ErrorActionPreference = 'Stop'

$projDir = $PSScriptRoot
Set-Location $projDir

# Minecraft 1.21.11 требует Java 21+; берём рантайм из Prism Launcher, если он есть.
$prism = Join-Path $env:APPDATA 'PrismLauncher\java\java-runtime-epsilon'
if (Test-Path $prism) {
    $env:JAVA_HOME = $prism
}

Write-Host "JAVA_HOME = $env:JAVA_HOME"

& (Join-Path $projDir 'gradlew.bat') clean build --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Сборка не удалась.'
    exit $LASTEXITCODE
}

Get-ChildItem (Join-Path $projDir 'build\libs') -Filter '*.jar' |
    ForEach-Object { Write-Host "Готово: $($_.FullName) ($([math]::Round($_.Length / 1KB)) KB)" }

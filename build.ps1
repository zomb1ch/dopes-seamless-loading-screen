# Собирает мод под все поддерживаемые линии версий и складывает готовые jar в папку release.
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

# '' — корневой проект (1.21.9 - 1.21.11, с remapping), остальные — 26.x (без remapping).
$projects = @('', 'versions/26x', 'versions/262x', 'versions/263x')

foreach ($project in $projects) {
    $label = if ($project -eq '') { '<root>' } else { $project }
    Write-Host "==> Собираю $label"
    if ($project -eq '') {
        & (Join-Path $projDir 'gradlew.bat') clean build --console=plain
    } else {
        & (Join-Path $projDir 'gradlew.bat') --console=plain -p (Join-Path $projDir $project) clean build
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Сборка $label не удалась."
        exit $LASTEXITCODE
    }
}

# Собираем готовые jar (без -sources) из всех проектов в одну папку.
$releaseDir = Join-Path $projDir 'release'
if (Test-Path $releaseDir) {
    Remove-Item (Join-Path $releaseDir '*.jar') -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

$libDirs = @((Join-Path $projDir 'build\libs')) +
    ($projects | Where-Object { $_ -ne '' } | ForEach-Object { Join-Path $projDir "$_\build\libs" })

$jars = Get-ChildItem $libDirs -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-dev.jar' }

foreach ($jar in $jars) {
    Copy-Item $jar.FullName $releaseDir -Force
    Write-Host "Готово: release\$($jar.Name) ($([math]::Round($jar.Length / 1KB)) KB)"
}

Write-Host "Всего файлов: $($jars.Count) -> $releaseDir"


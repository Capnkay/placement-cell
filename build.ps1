# Builds the Campus Placement Cell into dist\placement.war
# No Maven or Gradle involved: the compiler is pointed straight at the Jakarta EE
# API jars that ship inside GlassFish, and jar packs the result as a WAR.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jdk = Join-Path $root 'server\jdk\jdk-21.0.12+8'
$glassfish = Join-Path $root 'server\glassfish7'
$modules = Join-Path $glassfish 'glassfish\modules'

$src = Join-Path $root 'src\main\java'
$webapp = Join-Path $root 'src\main\webapp'
$resources = Join-Path $root 'src\main\resources'
$build = Join-Path $root 'build'
$classes = Join-Path $build 'classes'
$staging = Join-Path $build 'web'
$dist = Join-Path $root 'dist'

if (-not (Test-Path $jdk)) { throw "JDK 21 not found at $jdk" }
if (-not (Test-Path $modules)) { throw "GlassFish modules not found at $modules" }

Write-Host 'Cleaning previous output' -ForegroundColor DarkGray
Remove-Item $build, $dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $classes, $staging, $dist | Out-Null

Write-Host 'Compiling Java sources' -ForegroundColor Cyan
$sources = Get-ChildItem $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
Write-Host "  $($sources.Count) source files"

$argfile = Join-Path $build 'sources.txt'
$sourceLines = $sources | ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' }
[System.IO.File]::WriteAllLines($argfile, $sourceLines, [System.Text.Encoding]::ASCII)

$cp = @(
    (Join-Path $modules '*'),
    (Join-Path $webapp 'WEB-INF\lib\*')
) -join ';'

& "$jdk\bin\javac.exe" `
    -encoding UTF-8 `
    --release 17 `
    -Xlint:-options `
    -proc:none `
    -parameters `
    -cp $cp `
    -d $classes `
    "@$argfile"

if ($LASTEXITCODE -ne 0) { throw "Compilation failed with exit code $LASTEXITCODE" }
Write-Host '  compiled cleanly' -ForegroundColor Green

Write-Host 'Assembling the web archive' -ForegroundColor Cyan
Copy-Item "$webapp\*" $staging -Recurse -Force

$classesTarget = Join-Path $staging 'WEB-INF\classes'
New-Item -ItemType Directory -Force $classesTarget | Out-Null
Copy-Item "$classes\*" $classesTarget -Recurse -Force

# persistence.xml has to land in WEB-INF/classes/META-INF for the container to find it
$metaTarget = Join-Path $classesTarget 'META-INF'
New-Item -ItemType Directory -Force $metaTarget | Out-Null
Copy-Item (Join-Path $resources 'META-INF\persistence.xml') $metaTarget -Force

$war = Join-Path $dist 'placement.war'
Push-Location $staging
try {
    & "$jdk\bin\jar.exe" --create --file $war .
    if ($LASTEXITCODE -ne 0) { throw "Packaging failed with exit code $LASTEXITCODE" }
}
finally { Pop-Location }

$size = [math]::Round((Get-Item $war).Length / 1MB, 2)
Write-Host "Built dist\placement.war ($size MB)" -ForegroundColor Green

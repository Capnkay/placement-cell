# Compiles and runs the test suite.
#
# No Maven or Gradle: javac is pointed at the Jakarta EE jars inside GlassFish
# plus the JUnit console launcher, and that same launcher discovers and runs the
# tests. The database tests skip themselves if MySQL is not running.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jdk = Join-Path $root 'server\jdk\jdk-21.0.12+8'
$modules = Join-Path $root 'server\glassfish7\glassfish\modules'
$junit = Join-Path $root 'server\test-lib\junit-platform-console-standalone-1.10.2.jar'
$driver = Join-Path $root 'server\drivers\mysql-connector-j-8.4.0.jar'

$mainSrc = Join-Path $root 'src\main\java'
$testSrc = Join-Path $root 'src\test\java'
$out = Join-Path $root 'build\test-classes'
$mainOut = Join-Path $root 'build\classes'

if (-not (Test-Path $junit)) { throw "The JUnit launcher is missing at $junit" }

Write-Host 'Compiling main sources' -ForegroundColor Cyan
Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $out, $mainOut | Out-Null

$mainFiles = Join-Path $root 'build\test-main-sources.txt'
Get-ChildItem $mainSrc -Recurse -Filter *.java |
        ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
        Set-Content $mainFiles -Encoding UTF8

& "$jdk\bin\javac.exe" -encoding UTF-8 --release 17 -proc:none -parameters `
        -cp "$modules\*;$(Join-Path $root 'src\main\webapp\WEB-INF\lib')\*" `
        -d $mainOut "@$mainFiles"
if ($LASTEXITCODE -ne 0) { throw 'Main sources failed to compile' }

Write-Host 'Compiling tests' -ForegroundColor Cyan
$testFiles = Join-Path $root 'build\test-sources.txt'
Get-ChildItem $testSrc -Recurse -Filter *.java |
        ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
        Set-Content $testFiles -Encoding UTF8

& "$jdk\bin\javac.exe" -encoding UTF-8 --release 17 -proc:none `
        -cp "$mainOut;$junit;$modules\*;$driver" `
        -d $out "@$testFiles"
if ($LASTEXITCODE -ne 0) { throw 'Tests failed to compile' }

Write-Host 'Running tests' -ForegroundColor Cyan
Write-Host ''

& "$jdk\bin\java.exe" -jar $junit execute `
        --class-path "$out;$mainOut;$modules\jakarta.persistence-api.jar;$driver" `
        --scan-class-path $out `
        --details=tree `
        --disable-banner

$code = $LASTEXITCODE
Write-Host ''
if ($code -eq 0) {
    Write-Host 'All tests passed' -ForegroundColor Green
} else {
    Write-Host "Tests failed with exit code $code" -ForegroundColor Red
}
exit $code

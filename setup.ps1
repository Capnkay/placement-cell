# One-time setup for a fresh clone.
#
# server\ is gitignored (it runs to hundreds of megabytes), so a teammate who
# just cloned the repo has none of it yet. This downloads exactly what run.ps1,
# build.ps1 and test.ps1 expect, into the exact paths they expect:
#
#   server\jdk\jdk-21.0.12+8                              private JDK 21
#   server\glassfish7                                     GlassFish 7.0.23
#   server\drivers\mysql-connector-j-8.4.0.jar             MySQL driver
#   server\test-lib\junit-platform-console-standalone-1.10.2.jar   test runner
#
# Run this once after cloning, then use run.ps1 / test.ps1 as normal. Safe to
# re-run: anything already in place is skipped.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$server = Join-Path $root 'server'
$tmp = Join-Path $server '_downloads'
New-Item -ItemType Directory -Force $server, $tmp | Out-Null

function Get-File([string]$url, [string]$dest) {
    if (Test-Path $dest) { return }
    Write-Host "Downloading $(Split-Path $dest -Leaf)..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing
}

# 1. JDK 21 (Eclipse Temurin build) -> server\jdk\jdk-21.0.12+8
$jdkTarget = Join-Path $server 'jdk\jdk-21.0.12+8'
if (-not (Test-Path $jdkTarget)) {
    $jdkZip = Join-Path $tmp 'jdk21.zip'
    Get-File 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12%2B8/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip' $jdkZip
    Write-Host 'Extracting JDK 21...' -ForegroundColor Cyan
    Expand-Archive $jdkZip -DestinationPath (Join-Path $server 'jdk') -Force
}
else {
    Write-Host 'JDK 21 already present' -ForegroundColor DarkGray
}

# 2. GlassFish 7.0.23 -> server\glassfish7
$glassfishTarget = Join-Path $server 'glassfish7'
if (-not (Test-Path $glassfishTarget)) {
    $gfZip = Join-Path $tmp 'glassfish.zip'
    Get-File 'https://github.com/eclipse-ee4j/glassfish/releases/download/7.0.23/glassfish-7.0.23.zip' $gfZip
    Write-Host 'Extracting GlassFish 7.0.23...' -ForegroundColor Cyan
    Expand-Archive $gfZip -DestinationPath $server -Force
}
else {
    Write-Host 'GlassFish 7.0.23 already present' -ForegroundColor DarkGray
}

# 3. MySQL Connector/J 8.4.0 -> server\drivers\mysql-connector-j-8.4.0.jar
$driverTarget = Join-Path $server 'drivers\mysql-connector-j-8.4.0.jar'
New-Item -ItemType Directory -Force (Split-Path $driverTarget) | Out-Null
Get-File 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar' $driverTarget

# 4. JUnit console launcher -> server\test-lib\junit-platform-console-standalone-1.10.2.jar
$junitTarget = Join-Path $server 'test-lib\junit-platform-console-standalone-1.10.2.jar'
New-Item -ItemType Directory -Force (Split-Path $junitTarget) | Out-Null
Get-File 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar' $junitTarget

Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ''
Write-Host 'Setup complete.' -ForegroundColor Green
Write-Host 'Next: create the database (see README.md), then run .\run.ps1' -ForegroundColor Green

# Starts the server and deploys the application.
#
#   1. puts the MySQL driver where the server can load it
#   2. checks MySQL is actually answering on 3306
#   3. starts the GlassFish domain
#   4. deploys the WAR at the context root /placement
#
# GlassFish and a private JDK live under server\ beside this script, so nothing
# is installed on the machine. MySQL is the one thing that has to be running
# already: this project uses the MySQL Community Server on localhost.

param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jdk = Join-Path $root 'server\jdk\jdk-21.0.12+8'
$glassfish = Join-Path $root 'server\glassfish7'
$asadmin = Join-Path $glassfish 'bin\asadmin.bat'
$domain = Join-Path $glassfish 'glassfish\domains\domain1'
$war = Join-Path $root 'dist\placement.war'
$serverLog = Join-Path $domain 'logs\server.log'
$driver = Join-Path $root 'server\drivers\mysql-connector-j-8.4.0.jar'

$env:JAVA_HOME = $jdk
$env:AS_JAVA = $jdk

function Test-Port([int]$port) {
    $client = New-Object Net.Sockets.TcpClient
    try { $client.Connect('127.0.0.1', $port); return $true }
    catch { return $false }
    finally { $client.Dispose() }
}

function Wait-ForPort([int]$port, [int]$seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

if (-not $SkipBuild) {
    & (Join-Path $root 'build.ps1')
}
if (-not (Test-Path $war)) { throw "dist\placement.war is missing, run build.ps1 first" }

# The JDBC driver has to be on the server's own classpath, not inside the WAR,
# because the connection pool is created by the container before the application
# is loaded.
#
# It goes in domain1\lib, which GlassFish reads into its common classloader at
# startup. Not domain1\lib\ext: that one relied on the JVM extension mechanism,
# which Java 9 removed, so a driver dropped there is silently never loaded.
$driverTarget = Join-Path $domain 'lib\mysql-connector-j-8.4.0.jar'
$driverJustCopied = $false
if (-not (Test-Path $driverTarget)) {
    if (-not (Test-Path $driver)) { throw "The MySQL driver is missing at $driver" }
    New-Item -ItemType Directory -Force (Split-Path $driverTarget) | Out-Null
    Copy-Item $driver $driverTarget
    $driverJustCopied = $true
    Write-Host 'Installed the MySQL driver into the domain' -ForegroundColor DarkGray
}

Write-Host ''
if (-not (Test-Port 3306)) {
    Write-Host 'MySQL is not answering on port 3306.' -ForegroundColor Red
    Write-Host 'Start the MySQL80 service, then run this script again.' -ForegroundColor Red
    exit 1
}
Write-Host 'MySQL is up on 3306' -ForegroundColor DarkGray

# A driver dropped in after the domain started is not visible to it, so restart.
if ($driverJustCopied -and (Test-Port 8080)) {
    Write-Host 'Restarting the domain so it picks up the driver' -ForegroundColor Cyan
    & $asadmin stop-domain domain1 2>&1 | Out-Null
    Start-Sleep -Seconds 3
}

if (Test-Port 8080) {
    Write-Host 'GlassFish is already running' -ForegroundColor DarkGray
}
else {
    Write-Host 'Starting the GlassFish domain' -ForegroundColor Cyan
    try {
        & $asadmin start-domain domain1 2>&1 |
                Where-Object { $_ -match '\S' } | ForEach-Object { "  $_" }
    }
    catch {
        # Some shells cannot hand the detached launcher a console and asadmin
        # reports that as a terminating error under $ErrorActionPreference =
        # 'Stop'. The fallback below (a hidden foreground process) is what
        # actually determines success, via Wait-ForPort.
        Write-Host "  $($_.Exception.Message)" -ForegroundColor Yellow
    }

    if (-not (Wait-ForPort 8080 20)) {
        # Some shells cannot hand the launcher a console, and the detached server
        # then dies on startup. Running it in the foreground of a hidden process
        # works everywhere, so that is the fallback rather than a dead end.
        Write-Host '  detached start did not take, running the server in a hidden process' -ForegroundColor Yellow
        Start-Process -FilePath $asadmin `
                      -ArgumentList 'start-domain', '--verbose', 'domain1' `
                      -WindowStyle Hidden
        if (-not (Wait-ForPort 8080 180)) {
            Write-Host 'The server did not come up. Last lines of the log:' -ForegroundColor Red
            Get-Content $serverLog -Tail 30
            exit 1
        }
    }
    Write-Host '  server is listening on 8080' -ForegroundColor Green
}

Write-Host 'Deploying placement.war' -ForegroundColor Cyan
& $asadmin deploy --force=true --contextroot placement --name placement $war 2>&1 |
        Where-Object { $_ -match '\S' } | ForEach-Object { "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host 'Deployment failed. The last lines of the server log:' -ForegroundColor Red
    Get-Content $serverLog -Tail 40
    exit 1
}

Write-Host ''
Write-Host '  Portal        http://localhost:8080/placement/' -ForegroundColor Green
Write-Host '  Admin console http://localhost:4848/' -ForegroundColor DarkGray
Write-Host ''
Write-Host '  Placement officer   tpo@campus.edu / Campus@2026' -ForegroundColor Gray
Write-Host '  Student             aarti.deshpande@campus.edu / Campus@2026' -ForegroundColor Gray
Write-Host ''
Write-Host '  Stop the server with .\stop.ps1' -ForegroundColor DarkGray

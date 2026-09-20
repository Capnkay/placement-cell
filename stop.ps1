# Shuts down the GlassFish domain started by run.ps1.
# MySQL is left alone: it is a Windows service that other things may be using.

$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
$jdk = Join-Path $root 'server\jdk\jdk-21.0.12+8'
$asadmin = Join-Path $root 'server\glassfish7\bin\asadmin.bat'

$env:JAVA_HOME = $jdk
$env:AS_JAVA = $jdk

Write-Host 'Stopping the GlassFish domain' -ForegroundColor Cyan
& $asadmin stop-domain domain1 2>&1 | ForEach-Object { "  $_" }

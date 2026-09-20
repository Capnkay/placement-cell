@echo off
rem  Shuts the GlassFish domain down. The database is untouched, so everything
rem  that was there is still there the next time start.bat runs.

setlocal
cd /d "%~dp0"

set "PS=powershell"
where pwsh >nul 2>&1 && set "PS=pwsh"

%PS% -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1"

echo.
pause

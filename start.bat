@echo off
rem  Campus Placement and Training Cell : one click start.
rem
rem  Double click this file, or run "start.bat" from a terminal in this folder.
rem  It compiles the project, starts the bundled GlassFish, deploys the WAR and
rem  opens the portal in the default browser.
rem
rem  The only thing that must already be running is MySQL on localhost:3306.
rem  GlassFish and the JDK live under server\ and nothing is installed.

setlocal
cd /d "%~dp0"

set "PS=powershell"
where pwsh >nul 2>&1 && set "PS=pwsh"

echo.
echo   Campus Placement and Training Cell
echo   starting the server, this takes about a minute the first time
echo.

%PS% -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
if errorlevel 1 goto failed

start "" "http://localhost:8080/placement/"

echo.
echo   Portal    http://localhost:8080/placement/
echo   Officer   tpo@campus.edu / Campus@2026
echo   Student   aarti.deshpande@campus.edu / Campus@2026
echo.
echo   Leave this window open or close it, the server keeps running either way.
echo   Shut the server down with stop.bat
echo.
pause
exit /b 0

:failed
echo.
echo   The application did not start. The reason is in the output above.
echo   The usual cause is the MySQL80 service being stopped.
echo.
pause
exit /b 1

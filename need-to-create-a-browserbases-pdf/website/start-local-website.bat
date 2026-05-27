@echo off
cd /d "%~dp0"

set PORT=4185
set URL=http://localhost:%PORT%/index.html
set BUNDLED_PY=C:\Users\MaheshChaturanga\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe

echo Starting Mahesh PDF Editor...
echo.

if exist "%BUNDLED_PY%" (
  start "Mahesh PDF Editor Server" cmd /k ""%BUNDLED_PY%" -m http.server %PORT% --bind 127.0.0.1"
  goto OPEN_SITE
)

where python >nul 2>nul
if %errorlevel%==0 (
  start "Mahesh PDF Editor Server" cmd /k "python -m http.server %PORT% --bind 127.0.0.1"
  goto OPEN_SITE
)

where py >nul 2>nul
if %errorlevel%==0 (
  start "Mahesh PDF Editor Server" cmd /k "py -m http.server %PORT% --bind 127.0.0.1"
  goto OPEN_SITE
)

echo Python was not found.
echo Please install Python, then run this file again.
pause
exit /b 1

:OPEN_SITE
timeout /t 3 /nobreak >nul
start "" "%URL%"

echo Opened:
echo %URL%
echo.
echo Keep the separate server window open while using the editor.
pause

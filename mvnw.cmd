@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\maven.ps1" %*
exit /b %ERRORLEVEL%

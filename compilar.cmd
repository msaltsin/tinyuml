@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar.ps1" %*
exit /b %ERRORLEVEL%

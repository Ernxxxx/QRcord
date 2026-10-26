@echo off
setlocal
cd /d "%~dp0"
set PYTHONNOUSERSITE=1
"%~dp0.venv\Scripts\python.exe" "%~dp0generate_icons.py" %*
exit /b %errorlevel%

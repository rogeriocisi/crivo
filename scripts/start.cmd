@echo off
rem Crivo (AIR Lab) - atalho para o prompt de comando do Windows.
rem Delega para start.ps1, que e o script unico de inicializacao.
rem Exemplos:
rem   scripts\start.cmd
rem   scripts\start.cmd -AppProfile prod
rem   scripts\start.cmd -Port 9090
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
exit /b %ERRORLEVEL%

@echo off
setlocal

rem Backward-compatible entrypoint for running one peer.
cd /d "%~dp0"
call "%~dp0run-peer.bat" %*

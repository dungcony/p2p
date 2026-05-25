@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem Run one peer. Without CLI parameters, the Swing app opens profile selection.
cd /d "%~dp0"

if "%~1"=="" (
    echo [INFO] No peer args supplied. Opening app profile selection.
    echo [TIP] Existing demo profiles: run-peer.bat --profile=alice ^| --profile=bob ^| --profile=carol
    set "EXEC_ARGS="
) else (
    set "EXEC_ARGS=%*"
)

echo [INFO] Peer args: %EXEC_ARGS%

echo [INFO] Preparing bootstrap-server test artifact for Maven resolution. This does not start bootstrap-server.
call mvn -pl bootstrap-server -am -DskipTests install
if errorlevel 1 (
    echo [ERROR] Could not prepare bootstrap-server dependency.
    exit /b 1
)

mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass="dungcony.ds.App" "-Dexec.args=%EXEC_ARGS%"

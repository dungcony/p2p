@echo off
setlocal

rem Always run Maven from the project root where pom.xml is located.
cd /d "%~dp0"
set "EXEC_ARGS=%*"

if "%EXEC_ARGS%"=="" (
    echo [INFO] No peer args supplied. Profile selection will choose the saved port or ask for a new profile port.
) else (
    echo [INFO] Peer args: %EXEC_ARGS%
)

mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App" "-Dexec.args=%EXEC_ARGS%"

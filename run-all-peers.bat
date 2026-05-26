@echo off
setlocal

rem Start bootstrap server and three demo peers: Alice, Bob, Carol.
cd /d "%~dp0"

set "DATA_ROOT=%~dp0runtime-data\peer-node"
set "ALICE_ARGS=--peer-name=Alice --peer-port=5001"
set "BOB_ARGS=--peer-name=Bob --peer-port=5002"
set "CAROL_ARGS=--peer-name=Carol --peer-port=5003"

if exist "%DATA_ROOT%\alice\config.properties" set "ALICE_ARGS=--profile=alice"
if exist "%DATA_ROOT%\bob\config.properties" set "BOB_ARGS=--profile=bob"
if exist "%DATA_ROOT%\carol\config.properties" set "CAROL_ARGS=--profile=carol"

echo [INFO] Preparing bootstrap-server test artifact for Maven resolution.
call mvn -pl bootstrap-server -am -DskipTests install
if errorlevel 1 (
    echo [ERROR] Could not prepare bootstrap-server dependency.
    exit /b 1
)

echo [INFO] Opening bootstrap server and three peer windows...
echo [INFO] Alice args: %ALICE_ARGS%
echo [INFO] Bob args: %BOB_ARGS%
echo [INFO] Carol args: %CAROL_ARGS%
start "P2P Bootstrap" /D "%~dp0" cmd /k run-bootstrap.bat
timeout /t 2 /nobreak > nul

start "P2P Alice" /D "%~dp0" cmd /k mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=%ALICE_ARGS%"
timeout /t 1 /nobreak > nul
start "P2P Bob" /D "%~dp0" cmd /k mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=%BOB_ARGS%"
timeout /t 1 /nobreak > nul
start "P2P Carol" /D "%~dp0" cmd /k mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=%CAROL_ARGS%"

echo [INFO] Started demo windows. Close each window when you are done.

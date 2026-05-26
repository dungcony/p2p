@echo off
setlocal

rem Start bootstrap server and three demo peers: Alice, Bob, Carol.
cd /d "%~dp0"

echo [INFO] Preparing bootstrap-server test artifact for Maven resolution.
call mvn -pl bootstrap-server -am -DskipTests install
if errorlevel 1 (
    echo [ERROR] Could not prepare bootstrap-server dependency.
    exit /b 1
)

echo [INFO] Opening bootstrap server and three peer windows...
start "P2P Bootstrap" /D "%~dp0" cmd /k run-bootstrap.bat
timeout /t 2 /nobreak > nul

start "P2P Alice" /D "%~dp0" cmd /k mvn -pl peer-node exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=--profile=alice"
timeout /t 1 /nobreak > nul
start "P2P Bob" /D "%~dp0" cmd /k mvn -pl peer-node exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=--profile=bob"
timeout /t 1 /nobreak > nul
start "P2P Carol" /D "%~dp0" cmd /k mvn -pl peer-node exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=--profile=carol"

echo [INFO] Started demo windows. Close each window when you are done.

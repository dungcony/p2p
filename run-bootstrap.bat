@echo off
setlocal

rem Run the standalone bootstrap/tracker server module.
cd /d "%~dp0"
mvn -pl bootstrap-server exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="9000"

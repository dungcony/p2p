@echo off
setlocal

rem Always run Maven from the project root where pom.xml is located.
cd /d "%~dp0"
mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App"

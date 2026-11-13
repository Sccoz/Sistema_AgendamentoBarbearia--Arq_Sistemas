@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd -q package
if errorlevel 1 exit /b 1
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -jar target\barbearia-1.0.0.jar
) else (
    java -jar target\barbearia-1.0.0.jar
)

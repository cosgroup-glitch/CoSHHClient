@echo off
setlocal
set "JAVA_EXE="

where java >nul 2>nul
if %errorlevel%==0 set "JAVA_EXE=java"

if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Java\jdk-*") do if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE for /d %%J in ("%ProgramFiles%\Microsoft\jdk-*") do if exist "%%~fJ\bin\java.exe" set "JAVA_EXE=%%~fJ\bin\java.exe"
if not defined JAVA_EXE if exist "%ProgramFiles%\Java\latest\bin\java.exe" set "JAVA_EXE=%ProgramFiles%\Java\latest\bin\java.exe"
if not defined JAVA_EXE if exist "%ProgramFiles(x86)%\Java\latest\bin\java.exe" set "JAVA_EXE=%ProgramFiles(x86)%\Java\latest\bin\java.exe"

if not defined JAVA_EXE (
    echo Java was not found. Install Java 21 or newer, then run this again.
    echo https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)

cd /d "%~dp0bin"
"%JAVA_EXE%" -Dsun.java2d.uiScale.enabled=false -Djava.net.preferIPv6Addresses=system -Dkami.modelscan=true --add-exports=java.base/java.lang=ALL-UNNAMED --add-exports=java.desktop/sun.awt=ALL-UNNAMED --add-exports=java.desktop/sun.java2d=ALL-UNNAMED -jar hafen.jar
endlocal

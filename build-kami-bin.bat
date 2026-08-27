@echo off
setlocal

set "ANT_CMD="

where ant >nul 2>nul
if %errorlevel%==0 set "ANT_CMD=ant"

if not defined ANT_CMD if exist "%USERPROFILE%\Desktop\haven client\.tools\apache-ant-1.10.14\bin\ant.bat" (
    set "ANT_CMD=%USERPROFILE%\Desktop\haven client\.tools\apache-ant-1.10.14\bin\ant.bat"
)

if not defined ANT_CMD (
    echo Apache Ant was not found on PATH or in the local Haven client tools folder.
    echo Install Ant or copy the .tools\apache-ant folder into this workspace.
    exit /b 1
)

call "%ANT_CMD%" bin
endlocal

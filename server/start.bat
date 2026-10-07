@echo off
rem ============================================================
rem  Minecraft Paper server - just double-click this file.
rem  The first start downloads everything it needs by itself.
rem ============================================================
rem  Change these if you want a different version or more memory.
set MC_VERSION=26.3
set RAM=4G
rem ============================================================

cd /d "%~dp0"
title Minecraft Paper Server %MC_VERSION%

rem --- Step 1: download Java and Paper if missing ---
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1" -McVersion %MC_VERSION%
if errorlevel 1 (
    echo.
    echo [!] Setup failed. Read the red message above.
    echo     Most of the time it is the internet connection - just try again.
    pause
    exit /b 1
)

rem --- Step 2: accept the Minecraft EULA - first start only ---
findstr /c:"eula=true" eula.txt >nul 2>nul
if errorlevel 1 call :askeula
if errorlevel 1 exit /b 1

rem --- Step 3: start the server ---
echo.
echo Starting the server with %RAM% of memory...
echo Type "stop" in this window to shut it down safely.
echo.
"%~dp0java\bin\java.exe" -Xms%RAM% -Xmx%RAM% -jar paper.jar nogui

echo.
echo The server has stopped.
pause
exit /b 0

:askeula
echo.
echo To run a Minecraft server you must agree to the Minecraft EULA:
echo     https://aka.ms/MinecraftEULA
echo.
set /p AGREE=Type YES and press Enter if you agree: 
if /i not "%AGREE%"=="YES" (
    echo You did not agree, so the server will not start.
    pause
    exit /b 1
)
echo eula=true> eula.txt
exit /b 0

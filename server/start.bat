@echo off
rem ============================================================
rem  Fabric Minecraft server - double-click this file to start
rem ============================================================
rem  Change these if you want a different version or more memory.
set MC_VERSION=26.2
set LOADER_VERSION=0.19.3
set RAM=4G
rem ============================================================

cd /d "%~dp0"
title Minecraft Fabric Server %MC_VERSION%

rem --- Step 1: make sure Java is installed ---
where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo [!] Java was not found on this PC.
    echo     Minecraft %MC_VERSION% needs Java 25 or newer.
    echo     Download it from https://adoptium.net/ , install it,
    echo     then double-click start.bat again.
    echo.
    pause
    exit /b 1
)

rem --- Step 2: download the Fabric server launcher (first run only) ---
if not exist fabric-server-launch.jar (
    echo Downloading the Fabric server launcher for Minecraft %MC_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference='Stop';" ^
      "$inst = (Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/installer' | Where-Object stable | Select-Object -First 1).version;" ^
      "Invoke-WebRequest ('https://meta.fabricmc.net/v2/versions/loader/%MC_VERSION%/%LOADER_VERSION%/' + $inst + '/server/jar') -OutFile 'fabric-server-launch.jar'"
    if errorlevel 1 (
        echo.
        echo [!] The download failed. Check your internet connection and try again.
        if exist fabric-server-launch.jar del fabric-server-launch.jar
        pause
        exit /b 1
    )
)

rem --- Step 3: accept the Minecraft EULA (first run only) ---
findstr /c:"eula=true" eula.txt >nul 2>nul
if errorlevel 1 (
    echo.
    echo To run a Minecraft server you must agree to the Minecraft EULA:
    echo     https://aka.ms/MinecraftEULA
    echo.
    set /p AGREE=Type YES and press Enter if you agree:
    call :checkagree
    if errorlevel 1 exit /b 1
)

rem --- Step 4: start the server ---
echo.
echo Starting the server with %RAM% of memory...
echo Type "stop" in this window to shut it down safely.
echo.
java -Xms%RAM% -Xmx%RAM% -jar fabric-server-launch.jar nogui

echo.
echo The server has stopped.
pause
exit /b 0

:checkagree
if /i not "%AGREE%"=="YES" (
    echo You did not agree, so the server will not start.
    pause
    exit /b 1
)
echo eula=true> eula.txt
exit /b 0

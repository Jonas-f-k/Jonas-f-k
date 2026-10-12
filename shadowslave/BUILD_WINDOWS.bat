@echo off
setlocal
cd /d "%~dp0"
echo Building Shadow Slave for Minecraft 1.21.1. A JDK 21 is required.
call gradlew.bat build --no-daemon
if errorlevel 1 (
  echo Build failed. Read the error above. Ensure JAVA_HOME points to JDK 21.
  pause
  exit /b 1
)
echo Finished: build\libs\shadowslave-0.6.0-rework.3.jar
pause

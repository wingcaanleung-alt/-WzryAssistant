@echo off

REM ******************************************************************************
REM * Gradle start up script for Windows
REM ******************************************************************************

REM Resolve links - %~f0 may be a link to gradlew.bat itself
set PRG=%~f0

REM Get standard environment variables
:default
if "%APPDATA%"=="" set APPDATA=%USERPROFILE%\AppData\Roaming
set PRGDIR=%~dp0

"%PRGDIR%gradlew" %*

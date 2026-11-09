@echo off
REM ==============================================================================
REM Texidor Build Script for Windows (cmd.exe)
REM ==============================================================================
REM Usage:
REM   build.bat [--uni | --arm | --x86] [--release | --debug]
REM ==============================================================================

setlocal enabledelayedexpansion

set ARCH=uni
set BUILD_TYPE=release

:parse_args
if "%~1"=="" goto after_parse
if /i "%~1"=="--uni" (
    set ARCH=uni
    shift
    goto parse_args
)
if /i "%~1"=="--arm" (
    set ARCH=arm
    shift
    goto parse_args
)
if /i "%~1"=="--x86" (
    set ARCH=x86
    shift
    goto parse_args
)
if /i "%~1"=="--x86-64" (
    set ARCH=x86
    shift
    goto parse_args
)
if /i "%~1"=="--release" (
    set BUILD_TYPE=release
    shift
    goto parse_args
)
if /i "%~1"=="--debug" (
    set BUILD_TYPE=debug
    shift
    goto parse_args
)
if /i "%~1"=="-h" (
    goto show_help
)
if /i "%~1"=="--help" (
    goto show_help
)

echo Unknown argument: %~1
echo Type 'build.bat --help' for usage.
exit /b 1

:show_help
echo Texidor Windows Build Script
echo Usage: build.bat [--uni ^| --arm ^| --x86] [--release ^| --debug]
echo.
echo Options:
echo   --uni       Build Universal APK (Default)
echo   --arm       Build for ARM
echo   --x86       Build for x86-64
echo   --release   Build Release APK (Default)
echo   --debug     Build Debug APK
exit /b 0

:after_parse
echo ==================================================
echo  Building Texidor
echo  Architecture : %ARCH%
echo  Build Type   : %BUILD_TYPE%
echo ==================================================

set GRADLE_CMD=gradlew.bat
if not exist "gradlew.bat" (
    where gradle >nul 2>&1
    if %errorlevel% equ 0 (
        set GRADLE_CMD=gradle
    ) else (
        echo Error: Neither gradlew.bat nor system gradle was found.
        exit /b 1
    )
)

if "%BUILD_TYPE%"=="release" (
    if not exist "my-upload-key.jks" (
        echo Generating release signing keystore (my-upload-key.jks)...
        keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass upload -keypass upload -dname "CN=Texidor, OU=Texidor, O=Texidor, L=City, ST=State, C=US"
    )
    if "%STORE_PASSWORD%"=="" set STORE_PASSWORD=upload
    if "%KEY_PASSWORD%"=="" set KEY_PASSWORD=upload
    set TASK=:app:assembleRelease
    set SRC_APK=app\build\outputs\apk\release\app-release.apk
) else (
    set TASK=:app:assembleDebug
    set SRC_APK=app\build\outputs\apk\debug\app-debug.apk
)

echo Running: %GRADLE_CMD% %TASK%
call %GRADLE_CMD% %TASK%
if %errorlevel% neq 0 (
    echo Build failed.
    exit /b %errorlevel%
)

if not exist "build-outputs" mkdir "build-outputs"
set FINAL_APK=build-outputs\texidor-%BUILD_TYPE%-%ARCH%.apk
copy /y "%SRC_APK%" "%FINAL_APK%" >nul

echo ==================================================
echo  BUILD SUCCESSFUL
echo  Output: %FINAL_APK%
echo ==================================================
endlocal

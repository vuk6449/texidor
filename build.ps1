<#
.SYNOPSIS
  Texidor Build Script for Windows PowerShell
.DESCRIPTION
  Builds Texidor APKs with specified architecture and build variant.
.PARAMETER Arch
  Target architecture: 'uni' (default), 'arm', or 'x86'.
.PARAMETER BuildType
  Target build variant: 'release' (default) or 'debug'.
.EXAMPLE
  .\build.ps1 -Arch uni -BuildType release
  .\build.ps1 --arm --debug
#>

param(
    [string]$Arch = "uni",
    [string]$BuildType = "release",
    [switch]$uni,
    [switch]$arm,
    [switch]$x86,
    [switch]$release,
    [switch]$debug
)

if ($arm) { $Arch = "arm" }
if ($x86) { $Arch = "x86" }
if ($uni) { $Arch = "uni" }
if ($debug) { $BuildType = "debug" }
if ($release) { $BuildType = "release" }

# Also handle raw args
foreach ($arg in $args) {
    switch ($arg.ToLower()) {
        "--uni"     { $Arch = "uni" }
        "--arm"     { $Arch = "arm" }
        "--x86"     { $Arch = "x86" }
        "--x86-64"  { $Arch = "x86" }
        "--release" { $BuildType = "release" }
        "--debug"   { $BuildType = "debug" }
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Building Texidor (PowerShell)" -ForegroundColor Cyan
Write-Host " Architecture : $Arch" -ForegroundColor Cyan
Write-Host " Build Type   : $BuildType" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$gradleCmd = ".\gradlew.bat"
if (-not (Test-Path $gradleCmd)) {
    if (Get-Command "gradle" -ErrorAction SilentlyContinue) {
        $gradleCmd = "gradle"
    } else {
        Write-Error "Neither .\gradlew.bat nor system gradle was found."
        exit 1
    }
}

if ($BuildType -eq "release") {
    if (-not (Test-Path "my-upload-key.jks")) {
        Write-Host "Generating release signing keystore (my-upload-key.jks)..." -ForegroundColor Yellow
        keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass upload -keypass upload -dname "CN=Texidor, OU=Texidor, O=Texidor, L=City, ST=State, C=US"
    }
    if (-not $env:STORE_PASSWORD) { $env:STORE_PASSWORD = "upload" }
    if (-not $env:KEY_PASSWORD) { $env:KEY_PASSWORD = "upload" }
    $task = ":app:assembleRelease"
    $srcApk = "app\build\outputs\apk\release\app-release.apk"
} else {
    $task = ":app:assembleDebug"
    $srcApk = "app\build\outputs\apk\debug\app-debug.apk"
}

Write-Host "Running: $gradleCmd $task" -ForegroundColor Green
& $gradleCmd $task
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed."
    exit $LASTEXITCODE
}

$outDir = "build-outputs"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
$finalApk = "$outDir\texidor-$BuildType-$Arch.apk"
Copy-Item -Path $srcApk -Destination $finalApk -Force

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host " Output: $finalApk" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan

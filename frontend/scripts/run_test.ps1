<#
.SYNOPSIS
    Compiles, Installs, Launches and Injects a simulated SMS into the CIPHER Honeypot.
.DESCRIPTION
    Fully deterministic local pipeline utilizing ADB and the Gradle Wrapper to trigger
    a local honeypot execution cycle entirely on a physical Android device.
#>

param(
    [string]$Sender = "+18005559999",
    [string]$Message = "URGENT: Your Netflix account is suspended. Click here to update billing: https://netflix-update.ru",
    [switch]$RealSms
)

$ErrorActionPreference = "Stop"
$AppPackage = "com.cipher.security"
$DebugReceiver = "com.cipher.security.debug.DebugSmsReceiver"
$IntentAction = "com.cipher.security.DEBUG_INJECT_SMS"

function Write-Step {
    param([string]$Text)
    Write-Host ""
    Write-Host "[>] $Text" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Text)
    Write-Host "[OK] $Text" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Text)
    Write-Host "[FAIL] $Text" -ForegroundColor Red
}

# ── Step 1: Verify device ──
Write-Step "Confirming target device..."
$adbOutput = & adb devices
$deviceLine = $adbOutput | Select-String -Pattern "\tdevice$"
if (-not $deviceLine) {
    Write-Fail "No physical device detected. Check USB debugging."
    exit 1
}
Write-Success "Device validated."

# ── Step 2: Build ──
Write-Step "Building Debug APK..."
& .\gradlew assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Gradle Build Failed."
    exit $LASTEXITCODE
}
Write-Success "APK compiled."

# ── Step 3: Install ──
Write-Step "Installing Debug APK via ADB..."
& .\gradlew installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Fail "ADB Installation Failed."
    exit $LASTEXITCODE
}
Write-Success "Application installed."

# ── Step 4: Launch ──
Write-Step "Waking device and launching app..."
& adb shell input keyevent 82
& adb shell monkey -p $AppPackage -c android.intent.category.LAUNCHER 1
Start-Sleep -Seconds 3

# Clear log buffer before injection
& adb logcat -c

# ── Step 5: Inject ──
if ($RealSms) {
    Write-Step "Executing REAL SMS Injection via Twilio API..."
    & .\scripts\twilio_sms.ps1 -TargetPhone "+YOUR_PHYSICAL_NUMBER" -Body $Message
}
else {
    Write-Step "Injecting broadcast intent..."
    $componentName = "$AppPackage/$DebugReceiver"
    & adb shell am broadcast -a $IntentAction -n $componentName --es sender $Sender --es body $Message
}

Start-Sleep -Seconds 2

# ── Step 6: Stream logs ──
Write-Step "Streaming filtered logcat (Ctrl+C to quit)..."
$tagFilter = "DebugSmsReceiver:D SmsProcessingWorker:D ThreatRepository:D FeatureFlagManager:D EngagementWorker:D CipherWebSocketClient:D *:S"
& adb logcat -v color $tagFilter

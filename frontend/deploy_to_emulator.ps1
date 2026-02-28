# Configuration Variables
$AVD_NAME = "Pixel_API_34"
$PACKAGE_NAME = "com.cipher.security"
$MAIN_ACTIVITY = "com.cipher.security.MainActivity"
$BUILD_TYPE = "Debug"

# Constants
$EMULATOR_CMD = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
$ADB_CMD = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$GRADLE_CMD = ".\gradlew.bat"
$WAIT_TIMEOUT = 120
$RETRY_COUNT = 1

# Helper function for logging
function Log-Info {
    param([string]$Message)
    $time = Get-Date -Format "HH:mm:sc"
    Write-Host "`n[$time] $Message" -ForegroundColor Cyan
}

# Helper function for error handling
function Error-Exit {
    param([string]$Message)
    $time = Get-Date -Format "HH:mm:sc"
    Write-Host "`n[$time] ERROR: $Message" -ForegroundColor Red
    exit 1
}

Log-Info "Starting Android Automation Script"

# 1. Detect whether an Android Emulator is running
$CONNECTED_DEVICE = & $ADB_CMD devices -l | Select-String "device product:"
if (-not $CONNECTED_DEVICE) {
    Log-Info "No running emulator detected. Starting AVD: $AVD_NAME..."
    
    # 2. Automatically start the specified AVD in background
    Start-Process -NoNewWindow -FilePath $EMULATOR_CMD -ArgumentList "-avd $AVD_NAME -no-snapshot-save"
    
    Log-Info "Waiting for emulator to boot (Timeout: ${WAIT_TIMEOUT}s)..."
    $BOOT_COMPLETED = $false
    $WAIT_TIME = 0
    
    while ($WAIT_TIME -lt $WAIT_TIMEOUT) {
        $BOOT_STATUS = & $ADB_CMD shell getprop sys.boot_completed 2>$null
        if ($BOOT_STATUS -match "1") {
            $BOOT_COMPLETED = $true
            break
        }
        Start-Sleep -Seconds 5
        $WAIT_TIME += 5
        Write-Host -NoNewline "."
    }
    Write-Host ""
    
    if (-not $BOOT_COMPLETED) {
        Error-Exit "Emulator failed to boot within $WAIT_TIMEOUT seconds."
    }
    Log-Info "Emulator fully booted and ready."
} else {
    Log-Info "Active device/emulator detected."
}

# 3. Build the Android app
Log-Info "Building $BUILD_TYPE APK..."
$gradleProcess = Start-Process -NoNewWindow -Wait -PassThru -FilePath $GRADLE_CMD -ArgumentList "assemble$BUILD_TYPE"
if ($gradleProcess.ExitCode -ne 0) {
    Error-Exit "Gradle build failed."
}
Log-Info "Build successful."

# Locate the APK
$BUILD_LOWER = $BUILD_TYPE.ToLower()
$APK_PATH = Get-ChildItem -Path "app\build\outputs\apk\$BUILD_LOWER" -Filter "*.apk" -Recurse | Select-Object -First 1 -ExpandProperty FullName

if ([string]::IsNullOrEmpty($APK_PATH)) {
    Error-Exit "APK file not found after build."
}

# 4. Install the generated APK
Log-Info "Installing $APK_PATH..."
$INSTALL_SUCCESS = $false

for ($i = 0; $i -le $RETRY_COUNT; $i++) {
    & $ADB_CMD install -r $APK_PATH
    if ($LASTEXITCODE -eq 0) {
        $INSTALL_SUCCESS = $true
        break
    } else {
        Log-Info "Install failed. Retrying... ($i/$RETRY_COUNT)"
        Start-Sleep -Seconds 2
    }
}

if (-not $INSTALL_SUCCESS) {
    Error-Exit "Failed to install the APK after retries."
}
Log-Info "Installation successful."

# 5. Launch the app automatically
Log-Info "Launching $PACKAGE_NAME..."
& $ADB_CMD shell am start -n "$PACKAGE_NAME/.MainActivity"
if ($LASTEXITCODE -ne 0) {
    Error-Exit "Failed to launch the application."
}

Log-Info "Application successfully launched. Automation complete."

#!/bin/bash

# Configuration Variables
AVD_NAME="Pixel_API_34"
PACKAGE_NAME="com.cipher.security"
MAIN_ACTIVITY="com.cipher.security.MainActivity"
BUILD_TYPE="Debug"

# Constants
EMULATOR_CMD="$HOME/AppData/Local/Android/Sdk/emulator/emulator"
ADB_CMD="$HOME/AppData/Local/Android/Sdk/platform-tools/adb"
GRADLE_CMD="./gradlew"
WAIT_TIMEOUT=120
RETRY_COUNT=1

# Helper function for logging
log() {
    echo -e "\n[$(date +'%H:%M:%S')] $1"
}

# Helper function for error handling
error_exit() {
    echo -e "\n[$(date +'%H:%M:%S')] ERROR: $1" >&2
    exit 1
}

# Fix for macOS/Linux standard paths if not on WSL/Windows
if [ ! -f "$EMULATOR_CMD" ]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        EMULATOR_CMD="$HOME/Library/Android/sdk/emulator/emulator"
        ADB_CMD="$HOME/Library/Android/sdk/platform-tools/adb"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        EMULATOR_CMD="$HOME/Android/Sdk/emulator/emulator"
        ADB_CMD="$HOME/Android/Sdk/platform-tools/adb"
    fi
fi

log "Starting Android Automation Script"

# 1. Detect whether an Android Emulator is running
CONNECTED_DEVICE=$($ADB_CMD devices -l | grep -v "List of devices attached" | grep "device ")
if [ -z "$CONNECTED_DEVICE" ]; then
    log "No running emulator detected. Starting AVD: $AVD_NAME..."
    
    # 2. Automatically start the specified AVD
    nohup $EMULATOR_CMD -avd "$AVD_NAME" -no-snapshot-save > /dev/null 2>&1 &
    
    log "Waiting for emulator to boot (Timeout: ${WAIT_TIMEOUT}s)..."
    BOOT_COMPLETED=0
    WAIT_TIME=0
    
    while [ $WAIT_TIME -lt $WAIT_TIMEOUT ]; do
        BOOT_STATUS=$($ADB_CMD shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        if [ "$BOOT_STATUS" == "1" ]; then
            BOOT_COMPLETED=1
            break
        fi
        sleep 5
        WAIT_TIME=$((WAIT_TIME + 5))
        echo -n "."
    done
    echo ""

    if [ $BOOT_COMPLETED -eq 0 ]; then
        error_exit "Emulator failed to boot within $WAIT_TIMEOUT seconds."
    fi
    log "Emulator fully booted and ready."
else
    log "Active device/emulator detected."
fi

# 3. Build the Android app
log "Building $BUILD_TYPE APK..."
$GRADLE_CMD assemble${BUILD_TYPE}
if [ $? -ne 0 ]; then
    error_exit "Gradle build failed."
fi
log "Build successful."

# Locate the APK safely handling the uppercase/lowercase variance
BUILD_LOWER=$(echo "$BUILD_TYPE" | tr '[:upper:]' '[:lower:]')
APK_PATH=$(find app/build/outputs/apk/$BUILD_LOWER -name "*.apk" | head -n 1)
if [ -z "$APK_PATH" ]; then
    error_exit "APK file not found after build."
fi

# 4. Install the generated APK
log "Installing $APK_PATH..."
INSTALL_SUCCESS=0
for ((i=0; i<=$RETRY_COUNT; i++)); do
    $ADB_CMD install -r "$APK_PATH"
    if [ $? -eq 0 ]; then
        INSTALL_SUCCESS=1
        break
    else
        log "Install failed. Retrying... ($i/$RETRY_COUNT)"
        sleep 2
    fi
done

if [ $INSTALL_SUCCESS -eq 0 ]; then
    error_exit "Failed to install the APK after retries."
fi
log "Installation successful."

# 5. Launch the app automatically
log "Launching $PACKAGE_NAME..."
$ADB_CMD shell am start -n "$PACKAGE_NAME/.MainActivity"
if [ $? -ne 0 ]; then
    error_exit "Failed to launch the application."
fi

log "Application successfully launched. Automation complete."

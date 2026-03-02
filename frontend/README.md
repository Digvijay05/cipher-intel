# CIPHER Android Application

The `frontend` directory contains the autonomous Android application for the CIPHER platform. It functions as the physical honeypot that intercepts, analyzes, and engages with SMS scams directly on the device.

## Architecture

The application is built using native Android technologies (Kotlin) with a robust background processing pipeline to ensure autonomous operation without user intervention.

### Core Pipeline

1.  **`SmsReceiver`**: A `BroadcastReceiver` that captures incoming SMS messages, extracts the text, sender information, and hardware details like the `subscriptionId` (to fully support dual-SIM Android devices). It immediately hands off processing to the WorkManager to prevent blocking the UI thread.
2.  **`SmsProcessingWorker`**: Runs the `LocalValidator` over the parsed text to score the message for scam properties (e.g., urgency, financial links, keywords) completely offline without hitting the network. If the score exceeds the local threshold, it enqueues the `EngagementWorker`.
3.  **`EngagementWorker`**: Makes a secure REST call to the CIPHER backend API to leverage Cloud LLMs. Receives an AI-generated conversational reply and dispatches it back to the exact same SIM (`SmsManager`/`subscriptionId`) the scam was received on.
4.  **`SmsDeliveryReceiver`**: Hooks into the Android Telephony manager to precisely log message delivery statuses (`SMS_SENT`, `SMS_DELIVERED`) and handle transmission failures (e.g., `GENERIC_FAILURE`, `NO_SERVICE`).

### Key Technologies

*   **Kotlin & Coroutines**: For asynchronous execution and structured concurrency.
*   **Android WorkManager**: For guaranteed background execution of the analysis and engagement pipeline.
*   **Jetpack Compose**: For the diagnostic and monitoring user interface.
*   **Room Database**: Local SQLite persistence for offline state, configuration flags, and fallback.
*   **Retrofit & OkHttp**: For communicating with the FastAPI backend, implementing circuit breakers, retry interceptors, and error handling.

## Build configuration

For security reasons, sensitive configurations such as API Keys and the Backend URL are managed by `.env` injection.

1.  Copy the `.env.example` in the project root to `.env` and fill it in.
    ```
    CIPHER_API_KEY=your_api_key_here
    CIPHER_BASE_URL=https://your-backend-domain.com/
    ```
2.  The `build.gradle.kts` extracts these values at compile-time and injects them securely into the application's `BuildConfig`.

## Setup & Deployment

### Emulator Testing (Debug)
Running `./gradlew assembleDebug` will build an APK that can be deployed onto an emulator or physical device.

If you test via `adb shell am broadcast` to trigger simulated SMS messages, ensure you bypass SMS routing restrictions and supply a valid `subscriptionId`. A provided script `scripts/loopback_test.ps1` runs the end-to-end flow.

### Production Release (Release)
Release builds are obfuscated via R8 (ProGuard). The configuration ensures components referenced via reflection by Android (like WorkManager and BroadcastReceivers) are not aggressively stripped.

You will need a release Keystore. Define the standard Gradle signing environment variables: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

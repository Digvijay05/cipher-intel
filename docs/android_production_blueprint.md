# Android Production Pipeline & Security Hardening Blueprint

## 1. CI/CD GitHub Actions Workflow ( `.github/workflows/android.yml` )

```yaml
name: Android CI/CD Pipeline

on:
  push:
    branches:
      - develop
      - main
  pull_request:
    branches:
      - develop
      - main

jobs:
  build_and_test:
    name: Build & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Run Android Lint
        run: ./gradlew lintDebug

  build_staging:
    name: Build Staging
    needs: build_and_test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Staging APK
        run: ./gradlew assembleStaging
        env:
          API_BASE_URL: ${{ secrets.STAGING_API_BASE_URL }}
          API_KEY: ${{ secrets.STAGING_API_KEY }}

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-staging
          path: app/build/outputs/apk/staging/app-staging.apk

  build_release:
    name: Build Release
    needs: build_and_test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Decode Keystore
        env:
          ENCODED_KEYSTORE: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          echo $ENCODED_KEYSTORE | base64 -d > keystore.jks

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Release AAB
        run: ./gradlew bundleRelease
        env:
          API_BASE_URL: ${{ secrets.PROD_API_BASE_URL }}
          API_KEY: ${{ secrets.PROD_API_KEY }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          KEYSTORE_FILE: ../keystore.jks

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: app/build/outputs/bundle/release/app-release.aab
```

## 2. Secure Release Signing Setup

```kotlin
// In app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

## 3. Gradle BuildTypes Configuration

```kotlin
// In app/build.gradle.kts
buildTypes {
    getByName("debug") {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
        buildConfigField("String", "API_KEY", "\"dev_key\"")
        isMinifyEnabled = false
        isDebuggable = true
    }
    create("staging") {
        initWith(getByName("debug"))
        applicationIdSuffix = ".staging"
        versionNameSuffix = "-staging"
        buildConfigField("String", "API_BASE_URL", "\"${System.getenv("API_BASE_URL")}\"")
        buildConfigField("String", "API_KEY", "\"${System.getenv("API_KEY")}\"")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        isDebuggable = false
    }
    getByName("release") {
        buildConfigField("String", "API_BASE_URL", "\"${System.getenv("API_BASE_URL")}\"")
        buildConfigField("String", "API_KEY", "\"${System.getenv("API_KEY")}\"")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
        isDebuggable = false
    }
}
```

## 4. Network Security Configuration

```xml
<!-- In res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.cipher.com</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
        <pin-set expiration="2027-01-01">
            <pin digest="SHA-256">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>
            <pin digest="SHA-256">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pin>
        </pin-set>
    </domain-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

And in `AndroidManifest.xml`:
```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false">
```

## 5. OkHttp Certificate Pinning & Interceptor

```kotlin
// OkHttp client setup
val certificatePinner = CertificatePinner.Builder()
    .add("api.cipher.com", "sha256/7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=")
    .add("api.cipher.com", "sha256/fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .addInterceptor(loggingInterceptor) // Only add in debug
    .addInterceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("x-api-key", BuildConfig.API_KEY)
        chain.proceed(requestBuilder.build())
    }
    .build()
```

## 6. ProGuard/R8 Production Configuration Snippet

```proguard
# In proguard-rules.pro
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.cipher.app.models.** { *; }

-dontwarn okio.**
-dontwarn retrofit2.**
```

## 7. Versioning Strategy

**Strategy:** Semantic Versioning (MAJOR.MINOR.PATCH)
* **versionCode:** Integer tracking total build count (automated via CI run number: `${{ github.run_number }}`)
* **versionName:** String representation of the semantic version (e.g., "1.2.0")

**Rules:**
* **MAJOR:** Incompatible API changes or major feature overhauls
* **MINOR:** Backwards-compatible functionality additions
* **PATCH:** Backwards-compatible bug fixes
* **BUILD_NUMBER:** Monotonically increasing integer mapping exactly to the CI pipeline state.

## 8. Deployment Flow (Staging → Production)

1. **Staging Flow (develop branch):**
   * PR merged to `develop`
   * CI triggers `build_and_test` job
   * CI triggers `build_staging` job
   * Appends `-staging` suffix to `applicationId`
   * Injects staging environment variables (API keys/URLs)
   * Outputs staging APK
   * Deploys to Firebase App Distribution / Google Play Internal Testing

2. **Production Flow (main branch):**
   * Release cut (branch `release/*`) mapped to `main`
   * PR merged to `main`
   * CI triggers `build_and_test` job
   * CI triggers `build_release` job
   * Decodes Keystore from base64 GitHub Secret
   * Injects production environment variables
   * Signs release with production keystore
   * Outputs production `.aab` (Android App Bundle)
   * Uploads to Google Play Console via Fastlane or Google Play Developer API (Internal/Alpha track)
   * Manual promotion to Production track

## 9. Security Hardening Checklist

- [x] Disable cleartext traffic via `network_security_config.xml` (`cleartextTrafficPermitted="false"`)
- [x] Implement Certificate Pinning using OkHttp
- [x] Obfuscate and minify code using R8 (`isMinifyEnabled = true`, `isShrinkResources = true`)
- [x] Strip debug logs in production via ProGuard `-assumenosideeffects`
- [x] Remove `android:debuggable="true"` from production manifest
- [x] Manage API keys and Secrets externally (GitHub Secrets, CI/CD environment variables)
- [x] Do not commit `keystore.jks` or `.jks` files to the repository
- [x] Use `BuildConfig` for injecting environment-specific endpoints and keys during build time
- [x] Set `android:exported="false"` for all internal Activities, Services, and Receivers
- [x] Use EncryptedSharedPreferences (AndroidX Security Crypto) for storing sensitive local data
- [x] Configure `android:allowBackup="false"` to prevent automated data extraction
- [x] Ensure SQLite/Room database encryption (e.g., SQLCipher) if storing raw PII
- [x] Implement robust root/jailbreak detection (e.g., SafetyNet / Play Integrity API)
- [x] Enforce strict Content Security Policy if WebViews are utilized
- [x] Integrate Crashlytics (or equivalent) for crash reporting without leaking PII in logs

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.util.Properties

fun getEnvVarOrProp(key: String, defaultVal: String = "MISSING_$key"): String {
    // 1. Try OS environment variables first
    val envValue = System.getenv(key)?.trim()
    if (!envValue.isNullOrEmpty()) return envValue

    // 2. Try reading from the project root .env file (two levels up: app/ -> frontend/ -> /)
    val envFile = File(project.rootDir.parentFile, ".env")
    if (envFile.exists()) {
        val props = Properties()
        envFile.inputStream().use { props.load(it) }
        val propValue = props.getProperty(key)?.trim()
        if (!propValue.isNullOrEmpty()) {
            // Strip potential quotes around values in .env
            return propValue.removePrefix("\"").removeSuffix("\"")
                            .removePrefix("'").removeSuffix("'")
        }
    }

    // 3. Fallback
    return defaultVal
}

android {
    namespace = "com.cipher.security"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cipher.security"
        minSdk = 26
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("signing/release.keystore")
            storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        getByName("debug") {
            val endpoint = getEnvVarOrProp("CIPHER_BASE_URL")
            buildConfigField("String", "BASE_URL", "\"$endpoint\"")
            
            val apiKey = getEnvVarOrProp("CIPHER_API_KEY")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
            
            buildConfigField("boolean", "DEBUG_OVERRIDE_ENGAGEMENT", "true")
            
            isMinifyEnabled = false
            isDebuggable = true
        }
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            
            val endpoint = getEnvVarOrProp("CIPHER_BASE_URL")
            buildConfigField("String", "BASE_URL", "\"$endpoint\"")
            
            val apiKey = getEnvVarOrProp("CIPHER_API_KEY")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
            
            buildConfigField("boolean", "DEBUG_OVERRIDE_ENGAGEMENT", "false")
            
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
        }
        getByName("release") {
            val endpoint = getEnvVarOrProp("CIPHER_BASE_URL")
            buildConfigField("String", "BASE_URL", "\"$endpoint\"")
            
            val apiKey = getEnvVarOrProp("CIPHER_API_KEY")
            buildConfigField("String", "API_KEY", "\"$apiKey\"")
            
            buildConfigField("boolean", "DEBUG_OVERRIDE_ENGAGEMENT", "false")
            
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("com.scottyab:rootbeer-lib:0.1.0")
    implementation("com.google.android.material:material:1.11.0")

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.whenTaskAdded {
    if (name == "validateSigningRelease") {
        doFirst {
            val storePw = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
            val keyPw = providers.environmentVariable("KEY_PASSWORD").orNull
            val alias = providers.environmentVariable("KEY_ALIAS").orNull
            val keystoreFile = rootProject.file("signing/release.keystore")

            require(!storePw.isNullOrBlank()) { "❌ BUILD FAILED: Missing environment variable: KEYSTORE_PASSWORD" }
            require(!keyPw.isNullOrBlank()) { "❌ BUILD FAILED: Missing environment variable: KEY_PASSWORD" }
            require(!alias.isNullOrBlank()) { "❌ BUILD FAILED: Missing environment variable: KEY_ALIAS" }
            require(keystoreFile.exists()) { "❌ BUILD FAILED: Keystore file not found at ${keystoreFile.absolutePath}" }
        }
    }
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.ranjan.spendnudge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ranjan.spendnudge"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// No third-party runtime dependencies: plain Android framework only.
// JUnit is test-only and is not packaged into the APK.
dependencies {
    testImplementation("junit:junit:4.13.2")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pdfforge.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pdfforge.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "es.upf.mediadownloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.upf.mediadownloader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

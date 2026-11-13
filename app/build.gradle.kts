
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
        versionCode = 2
        versionName = "0.2.0"
    }

    // Configuración de Java
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Configuración de Kotlin
    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    implementation("androidx.browser:browser:1.8.0")
}

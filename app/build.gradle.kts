plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.horacerta.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.horacerta.app"
        minSdk = 24
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("horacerta.keystore")
            storePassword = "horacerta123"
            keyAlias = "horacerta"
            keyPassword = "horacerta123"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

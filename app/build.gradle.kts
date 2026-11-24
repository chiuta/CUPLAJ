plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ro.trom.cuplaj"
    compileSdk = 35

    defaultConfig {
        applicationId = "ro.trom.cuplaj"
        minSdk = 24
        targetSdk = 34
        versionCode = 150
        versionName = "1.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // APK instalabil direct (semnat cu cheia de depanare); pentru Play Store se înlocuiește cu cheia proprie.
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
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

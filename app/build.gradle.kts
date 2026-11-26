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
        versionCode = 1100
        versionName = "1.10.0"
    }

    // Stable release key: set CUPLAJ_KEYSTORE (path), CUPLAJ_KEYSTORE_PASSWORD, CUPLAJ_KEY_ALIAS, CUPLAJ_KEY_PASSWORD.
    // Without them the build falls back to the debug key (installable, but flagged by Play Protect and not updatable across builds).
    val ksPath = System.getenv("CUPLAJ_KEYSTORE")
    if (!ksPath.isNullOrEmpty() && file(ksPath).exists()) {
        signingConfigs.create("cuplajRelease") {
            storeFile = file(ksPath)
            storePassword = System.getenv("CUPLAJ_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("CUPLAJ_KEY_ALIAS")
            keyPassword = System.getenv("CUPLAJ_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("cuplajRelease") ?: signingConfigs.getByName("debug")
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

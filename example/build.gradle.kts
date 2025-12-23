import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
}

// Load local.properties for API keys (gitignored)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.dynalinks.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dynalinks.example"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Read from local.properties with fallback defaults
        buildConfigField(
            "String",
            "DYNALINKS_API_KEY",
            "\"${localProperties.getProperty("dynalinks.apiKey", "")}\""
        )
        buildConfigField(
            "String",
            "DYNALINKS_BASE_URL",
            "\"${localProperties.getProperty("dynalinks.baseUrl", "https://dynalinks.app/api/v1")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":dynalinks-sdk"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}

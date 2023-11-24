plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply(from = "deps.gradle")

android {
    namespace = "com.fourleaflabs.newcloversdk2"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.fourleaflabs.newcloversdk2"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    android.buildFeatures.buildConfig = true

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_KEY", "${project.findProperty("API_KEY_PROD")}")
            buildConfigField("String", "API_SECRET", "${project.findProperty("API_SECRET_PROD")}")
            buildConfigField("String", "APP_ID", "${project.findProperty("APP_ID")}")
            buildConfigField("String", "OAUTH", "${project.findProperty("OAUTH_TOKEN")}")
        }

        debug {
            buildConfigField("String", "API_KEY", "${project.findProperty("API_KEY_SANDBOX")}")
            buildConfigField("String", "API_SECRET", "${project.findProperty("API_SECRET_SANDBOX")}")
            buildConfigField("String", "APP_ID", "${project.findProperty("APP_ID")}")
            buildConfigField("String", "OAUTH", "${project.findProperty("OAUTH_TOKEN")}")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(files("libs/go-sdk-1.0.1.aar"))
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.anrola.onmyway"
    compileSdk = 35

    signingConfigs {
        create("androiddebug") {
            storeFile = file("D:\\IDE\\AndroidStudio\\KeyStore\\myKeyStore.jks")
            storePassword = "197319731973"
            keyAlias = "androiddebug"
            keyPassword = "197319731973"
        }
        create("androidrelease") {
            storeFile = file("D:\\IDE\\AndroidStudio\\KeyStore\\myKeyStore.jks")
            storePassword = "197319731973"
            keyPassword = "197319731973"
            keyAlias = "androidrelease"
        }

    }

    defaultConfig {
        applicationId = "com.anrola.onmyway"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Build Config
        // IF ERROR，CHECK THE gradle.properties FILE，
        // Need
        // SERVICE_IP = xxx.xxx.xxx.xxx
        // SERVICE_PORT = y
        // AI_API_KEY = z
        // AMAP_API_KEY = a
        buildConfigField("String", "SERVICE_IP", "\"${project.properties["SERVICE_IP"] ?: ""}\"")
        buildConfigField("String", "SERVICE_PORT", "\"${project.properties["SERVICE_PORT"] ?: ""}\"")
        buildConfigField("String", "AI_API_KEY", "\"${project.properties["AI_API_KEY"] ?: ""}\"")
        buildConfigField("String", "AMAP_API_KEY", "\"${project.properties["AMAP_API_KEY"] ?: ""}\"")



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("androiddebug")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("androidrelease")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("androiddebug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "assets/location_map_gps_locked.png"
        }
    }
}



dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    implementation(libs.circleimageview)
    implementation(libs.cardview)
    implementation(libs.mpandroidChart)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.amap3d)
    implementation (libs.okhttp3)
}


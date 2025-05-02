plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hci.ninjafruitgame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hci.ninjafruitgame"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        } 
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            val versionName = this.versionName   // Lấy versionName từ variant
            outputs.all {
                val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                outputImpl.outputFileName = "Ninja-Fruit-v$versionName.apk"
            }
        }
    }


}

dependencies {
    val cameraxVersion = "1.2.0-alpha02"
    // CameraX core library
    implementation("androidx.camera:camera-core:$cameraxVersion")

    // CameraX Camera2 extensions
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    // CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")

    // CameraX View class
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // MediaPipe Tasks Vision SDK (Google AI)
    implementation("com.google.mediapipe:tasks-vision:latest.release")
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta5")
    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    // Coroutine (nếu cần dùng async sau này)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
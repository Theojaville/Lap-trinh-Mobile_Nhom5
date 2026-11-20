/*
 * app/build.gradle.kts — ProsumerChat
 * !!! File này dễ conflict — ai thêm dependency phải báo nhóm trước !!!
 *
 * SETUP FIREBASE (phải làm trước khi build):
 *   1. Tạo project Firebase tại https://console.firebase.google.com
 *   2. Thêm Android app với package: com.prosumerchat.app
 *   3. Tải file google-services.json về
 *   4. Đặt google-services.json vào thư mục: app/   (cùng cấp với file này)
 *   5. File google-services.json ĐÃ BỊ CHẶN trong .gitignore — KHÔNG commit lên git.
 *      Mỗi dev phải tự tải file thật về máy mình.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.prosumerchat.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.prosumerchat.app"
        minSdk        = 25
        targetSdk     = 37
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SQLCipher: load native library đúng ABI
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig  = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── AndroidX ───────────────────────────────────────────────
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.slidingpanelayout)

    // ── Material 3 ─────────────────────────────────────────────
    implementation(libs.material)

    // ── Lifecycle — ViewModel + LiveData ───────────────────────
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // ── Firebase (BoM kiểm soát version) ───────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

    // ── Room (annotationProcessor vì dùng Java thuần) ──────────
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.paging)

    // ── Paging ─────────────────────────────────────────────────
    implementation(libs.paging.runtime)
    implementation(libs.paging.guava)

    // ── Security / Crypto ──────────────────────────────────────
    implementation(libs.tink.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.android)

    // ── Biometric ──────────────────────────────────────────────
    implementation(libs.biometric)

    // ── Glide ──────────────────────────────────────────────────
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // ── Media3 (ExoPlayer) ─────────────────────────────────────
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // ── Cloudinary ─────────────────────────────────────────────
    implementation(libs.cloudinary.android)

    // ── Test ───────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
}

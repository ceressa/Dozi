plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

// 🎯 Profesyonel Versiyonlama Sistemi
// ────────────────────────────────────────────────────────────────
// Semantic Versioning: MAJOR.MINOR.PATCH[-STAGE][+BUILD]
// Örnek: 0.1.0-beta+241

object AppVersion {
    const val MAJOR = 0          // Breaking changes
    const val MINOR = 1          // New features, backwards compatible
    const val PATCH = 0          // Bug fixes

    // 📋 Test Aşamaları:
    // "alpha"      -> Internal Testing (Dahili Test)
    // "beta"       -> Closed Testing (Kapalı Test) ✅ Şu an buradasınız
    // "rc"         -> Open Testing (Açık Test / Release Candidate)
    // "production" -> Production (Canlı Yayın)
    const val RELEASE_STAGE = "beta"  // 🔵 Kapalı Test

    const val INCLUDE_BUILD_NUMBER = true  // APK isminde build numarası göster
}

// 📊 Git commit sayısını al (versionCode için)
fun getGitCommitCount(): Int {
    return try {
        val process = Runtime.getRuntime().exec("git rev-list --count HEAD")
        process.waitFor()
        val output = process.inputStream.bufferedReader().readText().trim()
        output.toIntOrNull() ?: 1
    } catch (e: Exception) {
        println("⚠️ Git commit sayısı alınamadı, varsayılan değer kullanılıyor: ${e.message}")
        1
    }
}

// 🏷️ Version Name oluştur (Semantic Versioning)
fun getVersionName(): String {
    val baseVersion = "${AppVersion.MAJOR}.${AppVersion.MINOR}.${AppVersion.PATCH}"
    val stageSuffix = when (AppVersion.RELEASE_STAGE) {
        "production" -> ""
        else -> "-${AppVersion.RELEASE_STAGE}"
    }
    val buildSuffix = if (AppVersion.INCLUDE_BUILD_NUMBER && AppVersion.RELEASE_STAGE != "production") {
        ".${getGitCommitCount()}"
    } else {
        ""
    }
    return "$baseVersion$stageSuffix$buildSuffix"
}

android {
    namespace = "com.bardino.dozi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bardino.dozi"
        minSdk = 24
        targetSdk = 35
        versionCode = getGitCommitCount()  // Build number (her commit +1)
        versionName = getVersionName()      // Semantic version (0.1.0-beta.241)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    // 📦 APK dosya ismi (Profesyonel format)
    // Örnek: Dozi_v0.1.0-beta.241_Release_build-241.apk
    android.applicationVariants.all {
        outputs.all {
            val appName = "Dozi"
            val version = defaultConfig.versionName
            val buildNumber = defaultConfig.versionCode
            val variantName = name.replaceFirstChar { it.uppercase() }
            val newName = "${appName}_v${version}_${variantName}_build-${buildNumber}.apk"
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = newName
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = "1.5.15"

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    // 🎯 Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6") // ✅ collectAsState için
    implementation(libs.play.services.code.scanner)
    implementation(libs.androidx.material3)

    // 🎨 Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 🧭 Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 🌍 Maps & Location
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // 🧠 ML Kit OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // 💉 Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // 🗄️ Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 📦 DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 🔔 Material ve Bildirim
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // ⏰ WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 🧩 JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // 🔥 Firebase (BoM ile)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // 🔑 Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // 💳 Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // 🖼️ Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 🔲 Glance (Widget)
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // 📸 ML Kit Barcode / QR Code Scanner (Play Services)
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // 🧪 Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.51.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0") // LiveData instant executor

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

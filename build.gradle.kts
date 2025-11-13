buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.gms:google-services:4.4.4")
    }
}

plugins {
    // Android Gradle Plugin (Compose 1.9.x ile uyumlu)
    id("com.android.application") version "8.6.1" apply false

    // Kotlin versiyonu 1.9.25
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false

    // KSP (Kotlin Symbol Processing)
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false

    // Hilt (Dependency Injection)
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

// Clean task
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

plugins {
    id("com.android.application")   // AGP 9부터 Kotlin 지원이 기본 포함
    kotlin("plugin.compose")        // Jetpack Compose 컴파일러
}

android {
    namespace = "com.wakeuphero.app"
    compileSdk {
        version = release(37) { minorApiLevel = 1 } // Compose 최신 버전이 37.1 이상을 요구
    }

    defaultConfig {
        applicationId = "com.wakeuphero.app"
        minSdk = 26        // Android 8.0 이상 (java.time 기본 지원)
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core")) // 알람 계산·근무표 로직

    val composeBom = platform("androidx.compose:compose-bom:2026.11.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

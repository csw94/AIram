pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AIram"

include(":core") // 순수 Kotlin: 알람 계산·근무표 로직 + 콘솔 프로그램 + 테스트
include(":app")  // Android 앱 (Jetpack Compose)

plugins {
    kotlin("jvm") version "2.4.21"
    application
}

group = "com.wakeme"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // 테스트 코드 작성용 (JUnit 5 기반 kotlin.test)
    testImplementation(kotlin("test"))
}

kotlin {
    // 설치된 JDK로 빌드하되, 결과물은 Android에서도 쓰기 좋은 Java 17 기준으로 만든다
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

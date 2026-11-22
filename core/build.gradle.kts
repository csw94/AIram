plugins {
    kotlin("jvm")
    application
}

group = "com.wakeuphero"
version = "0.1.0"

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

// 콘솔 프로그램: ./gradlew :core:run
application {
    mainClass.set("com.wakeuphero.core.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir // schedule.txt를 프로젝트 맨 위 폴더에서 찾도록
}

tasks.test {
    useJUnitPlatform()
}

// 루트 프로젝트: 각 모듈이 쓸 플러그인 버전만 정해 둔다 (apply false = 여기서는 적용 안 함)
plugins {
    id("com.android.application") version "9.5.2" apply false
    kotlin("jvm") version "2.4.21" apply false
    kotlin("plugin.compose") version "2.4.21" apply false
}

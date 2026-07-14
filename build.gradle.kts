// 분산 트랜잭션 학습 프로젝트 — 2PC vs Saga (회원 마이그레이션 시나리오)
// 플러그인 버전은 루트에서만 선언(apply false), 각 모듈에서 apply.
plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    kotlin("plugin.jpa") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    // Java 25 toolchain 통일 (study-transaction 과 동일)
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}

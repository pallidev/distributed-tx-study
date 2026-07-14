// common: 공통 도메인 모델 (회원, 이벤트, 보상 인터페이스)
// Boot 실행 모듈이 아니므로 spring-boot 플러그인은 제외, dependency-management 로 버전만 관리.
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

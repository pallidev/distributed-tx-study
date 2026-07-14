// two-pc: 2PC(JTA + Atomikos) 로 legacy/new 두 DB 에 원자적 쓰기.
//  Phase 1(prepare) → Phase 2(commit). 실패 시 자동 재시도(= 블로킹) 를 코드로 체험.
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jta-atomikos") // JTA 코디네이터
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

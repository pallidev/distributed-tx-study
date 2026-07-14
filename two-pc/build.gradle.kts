// two-pc: 2PC(JTA + Atomikos) 로 legacy/new 두 DB 에 원자적 쓰기.
//  Phase 1(prepare) → Phase 2(commit). 실패 시 자동 재시도(= 블로킹) 를 코드로 체험.
plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Boot 4 용 Atomikos 공식 스타터 (auto-config 포함) + XA JDBC
    implementation("com.atomikos:transactions-spring-boot4:6.0.1")
    implementation("com.atomikos:transactions-jdbc:6.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

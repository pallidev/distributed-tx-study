package com.example.dtx.twopc

import com.example.dtx.twopc.domain.legacy.LegacyMember
import com.example.dtx.twopc.domain.newdb.NewMember
import com.example.dtx.twopc.repository.legacy.LegacyMemberRepository
import com.example.dtx.twopc.repository.newdb.NewMemberRepository
import com.example.dtx.twopc.service.MemberMigrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

/**
 * 2PC 시나리오 검증 — **실제 MySQL XA**(Testcontainers 컨테이너 2개) 로 Atomikos 가 동작.
 *
 *  1. 정상: JTA 로 양쪽 DB 원자적 커밋
 *  2. 롤백: 비즈니스 예외 시 양쪽 모두 롤백
 *
 *  ※ Phase 2 커밋 단계 실패(한쪽 DB commit 실패) → Atomikos 자동 재시도(oltp_max_retries=5) 는
 *     실제 컨테이너 장애 주입이 필요해 여기서는 다루지 않는다(README 의 "2PC Phase 2 실패" 섹션 참고).
 */
@Testcontainers
@SpringBootTest
class MemberMigrationServiceTest {

    @Autowired private lateinit var svc: MemberMigrationService
    @Autowired private lateinit var newRepo: NewMemberRepository
    @Autowired private lateinit var legacyRepo: LegacyMemberRepository

    @Test
    fun `정상 흐름 - 2PC 로 양쪽 DB 모두 커밋된다`() {
        val email = "ok-${System.nanoTime()}@example.com"
        svc.createMember(email, "정상")

        assertThat(newRepo.findAll().map(NewMember::email)).contains(email)
        assertThat(legacyRepo.findAll().map(LegacyMember::email)).contains(email)
    }

    @Test
    fun `롤백 흐름 - 예외 발생 시 양쪽 DB 모두 롤백된다 (데이터 없음)`() {
        val email = "fail-${System.nanoTime()}@example.com"

        assertThrows<IllegalStateException> { svc.createMemberThenFail(email, "실패") }

        assertThat(newRepo.findAll().map(NewMember::email)).doesNotContain(email)
        assertThat(legacyRepo.findAll().map(LegacyMember::email)).doesNotContain(email)
    }

    companion object {
        // legacy / new 각각 독립 MySQL 컨테이너 (XA 지원)
        @Container
        @JvmStatic
        val legacy = MySQLContainer("mysql:8.0")

        @Container
        @JvmStatic
        val newdb = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun mysqlProps(r: DynamicPropertyRegistry) {
            r.add("datasource.legacy.url") { legacy.jdbcUrl }
            r.add("datasource.legacy.xa-class") { "com.mysql.cj.jdbc.MysqlXADataSource" }
            r.add("datasource.new.url") { newdb.jdbcUrl }
            r.add("datasource.new.xa-class") { "com.mysql.cj.jdbc.MysqlXADataSource" }
            r.add("datasource.user") { legacy.username }
            r.add("datasource.password") { legacy.password }
            // Hibernate 자동 DDL(hbm2ddl) 이 JTA global TX 안에서 commit 시도 → MySQL XA 거부.
            // 따라서 테스트에선 hbm2ddl=none 으로 두고, 스키마는 @BeforeAll 로 직접 생성.
            r.add("hibernate.hbm2ddl.auto") { "none" }
        }

        @BeforeAll
        @JvmStatic
        fun createSchema() {
            val ddl = """
                CREATE TABLE IF NOT EXISTS member (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  email VARCHAR(255),
                  name VARCHAR(255),
                  PRIMARY KEY (id)
                )
            """.trimIndent()
            DriverManager.getConnection(legacy.jdbcUrl, legacy.username, legacy.password).use { it.createStatement().execute(ddl) }
            DriverManager.getConnection(newdb.jdbcUrl, newdb.username, newdb.password).use { it.createStatement().execute(ddl) }
        }
    }
}

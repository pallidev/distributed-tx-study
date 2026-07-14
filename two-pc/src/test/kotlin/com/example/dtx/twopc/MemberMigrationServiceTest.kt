package com.example.dtx.twopc

import com.example.dtx.twopc.domain.contact.MemberContact
import com.example.dtx.twopc.domain.profile.MemberProfile
import com.example.dtx.twopc.repository.contact.MemberContactRepository
import com.example.dtx.twopc.repository.profile.MemberProfileRepository
import com.example.dtx.twopc.service.MemberMigrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
 * 2PC 시나리오 검증 — 실제 MySQL XA(Testcontainers 컨테이너 2개: profile / contact) 로 Atomikos 가 동작.
 *
 *  시나리오: 레거시 단일 회원(컬럼 다수) → 신규 DB 2개(profile/contact) 분할 저장.
 *  1. 정상: JTA 로 두 DB 원자적 커밋 (profile + contact 모두 저장)
 *  2. 롤백: contact 저장 직전 예외 → 두 DB 모두 롤백 (둘 다 없음)
 */
@Testcontainers
@SpringBootTest
class MemberMigrationServiceTest {

    @Autowired private lateinit var svc: MemberMigrationService
    @Autowired private lateinit var profileRepo: MemberProfileRepository
    @Autowired private lateinit var contactRepo: MemberContactRepository

    @Test
    fun `정상 흐름 - 2PC 로 profile·contact 두 DB 모두 커밋된다`() {
        val name = "ok-${System.nanoTime()}"
        val email = "ok-${System.nanoTime()}@example.com"

        val profileId = svc.registerMember(name, email, "010-1111-2222")

        assertThat(profileRepo.findById(profileId)).isPresent
        assertThat(profileRepo.findById(profileId).map(MemberProfile::name).orElse(null)).isEqualTo(name)
        val contact: MemberContact? = contactRepo.findByProfileId(profileId)
        assertThat(contact).isNotNull
        assertThat(contact!!.email).isEqualTo(email)
    }

    @Test
    fun `롤백 흐름 - 예외 발생 시 profile·contact 두 DB 모두 롤백된다`() {
        val name = "fail-${System.nanoTime()}"
        val email = "fail-${System.nanoTime()}@example.com"

        assertThrows<IllegalStateException> { svc.registerMemberThenFail(name, email, "010-9999-9999") }

        // profile 도 contact 도 해당 name/email 이 없어야
        assertThat(profileRepo.findAll().map(MemberProfile::name)).doesNotContain(name)
        assertThat(contactRepo.findAll().map(MemberContact::email)).doesNotContain(email)
    }

    companion object {
        @Container
        @JvmStatic
        val profile = MySQLContainer("mysql:8.0")

        @Container
        @JvmStatic
        val contact = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun mysqlProps(r: DynamicPropertyRegistry) {
            r.add("datasource.profile.url") { profile.jdbcUrl }
            r.add("datasource.profile.xa-class") { "com.mysql.cj.jdbc.MysqlXADataSource" }
            r.add("datasource.contact.url") { contact.jdbcUrl }
            r.add("datasource.contact.xa-class") { "com.mysql.cj.jdbc.MysqlXADataSource" }
            r.add("datasource.user") { profile.username }
            r.add("datasource.password") { profile.password }
            r.add("hibernate.hbm2ddl.auto") { "none" }
        }

        @BeforeAll
        @JvmStatic
        fun createSchema() {
            val ddl = listOf(
                """
                CREATE TABLE IF NOT EXISTS member_profile (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  name VARCHAR(255),
                  PRIMARY KEY (id)
                )
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS member_contact (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  profile_id BIGINT,
                  email VARCHAR(255),
                  phone VARCHAR(255),
                  PRIMARY KEY (id)
                )
                """.trimIndent(),
            )
            DriverManager.getConnection(profile.jdbcUrl, profile.username, profile.password).use { c ->
                ddl.forEach { c.createStatement().execute(it) }
            }
            DriverManager.getConnection(contact.jdbcUrl, contact.username, contact.password).use { c ->
                ddl.forEach { c.createStatement().execute(it) }
            }
        }
    }
}

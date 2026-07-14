package com.example.dtx.saga

import com.example.dtx.saga.domain.contact.MemberContact
import com.example.dtx.saga.domain.profile.MemberProfile
import com.example.dtx.saga.repository.contact.MemberContactRepository
import com.example.dtx.saga.repository.profile.MemberProfileRepository
import com.example.dtx.saga.saga.MemberMigrationSaga
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Saga 시나리오 검증 — 회원 분할 저장(profile/contact).
 *  1. 정상: Step1(profile) + Step2(contact) 모두 저장
 *  2. 보상: Step2 실패 → Step1 보상(profile DELETE). 양쪽 모두 해당 데이터 없음
 */
@SpringBootTest
class MemberMigrationSagaTest {

    @Autowired private lateinit var saga: MemberMigrationSaga
    @Autowired private lateinit var profileRepo: MemberProfileRepository
    @Autowired private lateinit var contactRepo: MemberContactRepository

    @Test
    fun `정상 흐름 - profile·contact 두 DB 모두 저장된다`() {
        val name = "ok-${System.nanoTime()}"
        val email = "ok-${System.nanoTime()}@example.com"

        val result = saga.registerMember(name, email, "010-1111-2222")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPLETED)
        assertThat(profileRepo.findAll().map(MemberProfile::name)).contains(name)
        assertThat(contactRepo.findAll().map(MemberContact::email)).contains(email)
    }

    @Test
    fun `보상 흐름 - Step2 실패시 Step1 이 보상(DELETE) 되어 양쪽 모두 데이터가 없다`() {
        val name = "fail-${System.nanoTime()}"
        val email = "fail-${System.nanoTime()}@example.com"

        val result = saga.registerMemberThenFail(name, email, "010-9999-9999")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPENSATED)
        assertThat(profileRepo.findAll().map(MemberProfile::name)).doesNotContain(name)
        assertThat(contactRepo.findAll().map(MemberContact::email)).doesNotContain(email)
    }
}

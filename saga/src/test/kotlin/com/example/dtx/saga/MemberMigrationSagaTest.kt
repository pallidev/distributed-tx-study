package com.example.dtx.saga

import com.example.dtx.saga.domain.contact.MemberContact
import com.example.dtx.saga.domain.profile.MemberProfile
import com.example.dtx.saga.repository.contact.MemberContactRepository
import com.example.dtx.saga.repository.profile.MemberProfileRepository
import com.example.dtx.saga.saga.MemberMigrationSaga
import com.example.dtx.saga.service.ContactService
import com.example.dtx.saga.service.ProfileService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Saga 시나리오 검증 — 회원 분할 저장/수정.
 *  ① INSERT 정상/보상(DELETE)
 *  ② UPDATE 정상/보상(before 이미지로 재 UPDATE) ★
 */
@SpringBootTest
class MemberMigrationSagaTest {

    @Autowired private lateinit var saga: MemberMigrationSaga
    @Autowired private lateinit var profileRepo: MemberProfileRepository
    @Autowired private lateinit var contactRepo: MemberContactRepository
    @Autowired private lateinit var profileSvc: ProfileService
    @Autowired private lateinit var contactSvc: ContactService

    @Test
    fun `① INSERT 정상 - profile·contact 두 DB 모두 저장된다`() {
        val name = "ok-${System.nanoTime()}"
        val email = "ok-${System.nanoTime()}@example.com"

        val result = saga.registerMember(name, email, "010-1111-2222")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPLETED)
        assertThat(profileRepo.findAll().map(MemberProfile::name)).contains(name)
        assertThat(contactRepo.findAll().map(MemberContact::email)).contains(email)
    }

    @Test
    fun `① INSERT 보상 - Step2 실패시 Step1 이 보상(DELETE) 되어 양쪽 모두 데이터가 없다`() {
        val name = "fail-${System.nanoTime()}"
        val email = "fail-${System.nanoTime()}@example.com"

        val result = saga.registerMemberThenFail(name, email, "010-9999-9999")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPENSATED)
        assertThat(profileRepo.findAll().map(MemberProfile::name)).doesNotContain(name)
        assertThat(contactRepo.findAll().map(MemberContact::email)).doesNotContain(email)
    }

    @Test
    fun `② UPDATE 보상 - Step2 실패시 Step1(profile name) 이 before 이미지로 되돌려진다`() {
        // 사전: profile + contact 생성 (UPDATE 대상)
        val originalName = "원래이름-${System.nanoTime()}"
        val originalEmail = "old-${System.nanoTime()}@example.com"
        val profileId = profileSvc.create(originalName)
        contactSvc.create(profileId, originalEmail, "010-0000-0000")

        // UPDATE 시도: profile name + contact email, contact 는 실패
        val result = saga.updateMemberNameAndEmail(profileId, newName = "새이름", newEmail = "new@x.com", failContact = true)

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPENSATED)
        // ★ UPDATE 보상: profile name 이 "새이름" 이 아니라 before 이미지 "원래이름" 으로 되돌려져 있어야
        val profile = profileRepo.findById(profileId).orElseThrow()
        assertThat(profile.name)
            .`as`("UPDATE 보상으로 before 이미지로 되돌려져야 (DELETE 가 아님)")
            .isEqualTo(originalName)
            .isNotEqualTo("새이름")
        // contact email 은 Step2 실패라 안 바뀜
        assertThat(contactRepo.findByProfileId(profileId)!!.email).isEqualTo(originalEmail)
    }
}

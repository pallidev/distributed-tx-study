package com.example.dtx.saga

import com.example.dtx.saga.domain.contact.MemberContact
import com.example.dtx.saga.domain.profile.MemberProfile
import com.example.dtx.saga.repository.contact.MemberContactRepository
import com.example.dtx.saga.repository.profile.MemberProfileRepository
import com.example.dtx.saga.service.ContactService
import com.example.dtx.saga.service.ProfileService
import com.example.dtx.saga.saga.MemberMigrationSaga
import com.example.dtx.saga.saga.MemberMigrationSaga.SagaResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest

/**
 * Saga 학습 시나리오 (Kotest BehaviorSpec — Given/When/Then).
 * SpringExtension 은 ProjectConfig 에서 전역 적용.
 *
 * 읽는 순서:
 *  ① INSERT 정상 → ② INSERT 보상(DELETE) → ③ UPDATE 보상(before 이미지 재UPDATE)
 */
@SpringBootTest
class MemberMigrationSagaSpec(
    private val saga: MemberMigrationSaga,
    private val profileRepo: MemberProfileRepository,
    private val contactRepo: MemberContactRepository,
    private val profileSvc: ProfileService,
    private val contactSvc: ContactService,
) : BehaviorSpec({

    given("회원 분할 저장(INSERT) 시나리오") {
        `when`("registerMember 로 정상 저장하면") {
            then("profile·contact 두 DB 모두 저장된다 (COMPLETED)") {
                val name = "ok-${System.nanoTime()}"
                val email = "ok-${System.nanoTime()}@example.com"

                val result = saga.registerMember(name, email, "010-1111-2222")

                result shouldBe SagaResult.COMPLETED
                profileRepo.findAll().map(MemberProfile::name) shouldContain name
                contactRepo.findAll().map(MemberContact::email) shouldContain email
            }
        }

        `when`("registerMemberThenFail 로 Step2(contact) 가 실패하면") {
            then("INSERT 보상 — Step1(profile) 이 DELETE 되어 양쪽 모두 데이터가 없다 (COMPENSATED)") {
                val name = "fail-${System.nanoTime()}"
                val email = "fail-${System.nanoTime()}@example.com"

                val result = saga.registerMemberThenFail(name, email, "010-9999-9999")

                result shouldBe SagaResult.COMPENSATED
                profileRepo.findAll().map(MemberProfile::name) shouldNotContain name
                contactRepo.findAll().map(MemberContact::email) shouldNotContain email
            }
        }
    }

    given("회원 분할 수정(UPDATE) 시나리오") {
        `when`("updateMemberNameAndEmail(fail) 로 Step2(contact) 가 실패하면") {
            then("UPDATE 보상 — Step1(profile name) 이 before 이미지로 재 UPDATE 되어 원래 이름으로 되돌려진다") {
                // 사전: profile + contact 생성 (UPDATE 대상)
                val originalName = "원래이름-${System.nanoTime()}"
                val originalEmail = "old-${System.nanoTime()}@example.com"
                val profileId = profileSvc.create(originalName)
                contactSvc.create(profileId, originalEmail, "010-0000-0000")

                // UPDATE 시도 (contact 실패 → 보상)
                val result = saga.updateMemberNameAndEmail(
                    profileId, newName = "새이름", newEmail = "new@x.com", failContact = true,
                )

                result shouldBe SagaResult.COMPENSATED
                val profile = profileRepo.findById(profileId).orElseThrow()
                profile.name shouldBe originalName   // ★ before 이미지로 되돌려짐 (새이름 아님)
                contactRepo.findByProfileId(profileId)!!.email shouldBe originalEmail // Step2 실패라 안 바뀜
            }
        }
    }
})

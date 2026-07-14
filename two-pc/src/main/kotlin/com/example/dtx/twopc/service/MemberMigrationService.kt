package com.example.dtx.twopc.service

import com.example.dtx.twopc.domain.contact.MemberContact
import com.example.dtx.twopc.domain.profile.MemberProfile
import com.example.dtx.twopc.repository.contact.MemberContactRepository
import com.example.dtx.twopc.repository.profile.MemberProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 2PC 기반 회원 분할 저장 서비스.
 *
 * 레거시 단일 회원(컬럼 다수)을 신규 DB 2개(profile/contact)로 분해해 저장.
 * 하나의 @Transactional(JTA) 안에서
 *  - 신규 DB A(profile) INSERT
 *  - 신규 DB B(contact) INSERT (profile.id 참조)
 * 를 모두 수행. Atomikos 가 Phase 1(prepare) → Phase 2(commit) 로 두 DB 를 원자적으로 묶는다.
 *
 *  - 정상: 두 DB 모두 커밋 (강한 일관성)
 *  - registerMemberThenFail: contact 저장 전 예외 → 두 DB 모두 롤백
 *    단, Phase 2 커밋 실패 시 이미 커밋된 쪽이 있어 롤백 불가 → Atomikos 자동 재시도(블로킹) (2PC 치명적 한계)
 */
@Service
class MemberMigrationService(
    private val profileRepo: MemberProfileRepository,
    private val contactRepo: MemberContactRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun registerMember(name: String, email: String, phone: String): Long {
        val profile = profileRepo.saveAndFlush(MemberProfile(name = name))
        contactRepo.saveAndFlush(MemberContact(profileId = profile.id!!, email = email, phone = phone))
        log.info("[2PC] 회원 분할 저장 완료 (profile+contact 모두 커밋) name={} profileId={}", name, profile.id)
        return profile.id!!
    }

    /** contact 저장 직전 실패 → 양쪽 롤백 시뮬레이션. */
    @Transactional
    fun registerMemberThenFail(name: String, email: String, phone: String) {
        val profile = profileRepo.saveAndFlush(MemberProfile(name = name))
        log.info("[2PC] profile 저장 후 contact 저장 전 예외 → 전체 롤백 profileId={}", profile.id)
        throw IllegalStateException("2PC: contact DB 저장 실패 시뮬레이션")
    }
}

package com.example.dtx.twopc.service

import com.example.dtx.twopc.domain.legacy.LegacyMember
import com.example.dtx.twopc.domain.newdb.NewMember
import com.example.dtx.twopc.repository.legacy.LegacyMemberRepository
import com.example.dtx.twopc.repository.newdb.NewMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 2PC 기반 회원 마이그레이션 서비스.
 *
 * 하나의 @Transactional(JTA) 안에서
 *  - 신규 DB(new) INSERT
 *  - 기존 DB(legacy) INSERT
 * 를 모두 수행. Atomikos 가 Phase 1(prepare) → Phase 2(commit) 로 양쪽 DB 를 원자적으로 묶는다.
 *
 * 학습 포인트:
 *  - 정상: 양쪽 모두 커밋 (강한 일관성)
 *  - createMemberThenFail: legacy 저장 직전 예외 → 양쪽 롤백.
 *    단, Phase 2 커밋 단계에서 한쪽이 실패하면 이미 커밋된 쪽이 있어 롤백 불가 →
 *    Atomikos 가 실패한 쪽에 COMMIT 을 자동 재시도(블로킹). (2PC 치명적 한계)
 */
@Service
class MemberMigrationService(
    private val legacyRepo: LegacyMemberRepository,
    private val newRepo: NewMemberRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createMember(email: String, name: String) {
        newRepo.save(NewMember(email = email, name = name))
        legacyRepo.save(LegacyMember(email = email, name = name))
        log.info("[2PC] 회원 동기화 완료 (신규+기존 모두 커밋) email={}", email)
    }

    /** legacy 저장 직전 실패 → 양쪽 롤백 시뮬레이션. */
    @Transactional
    fun createMemberThenFail(email: String, name: String) {
        newRepo.save(NewMember(email = email, name = name))
        log.info("[2PC] 신규 DB 저장 후 legacy 저장 전 예외 → 전체 롤백 email={}", email)
        throw IllegalStateException("2PC: legacy DB 저장 실패 시뮬레이션")
    }
}

package com.example.dtx.saga.service

import com.example.dtx.saga.domain.LegacyMember
import com.example.dtx.saga.repository.LegacyMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기존(legacy) DB 작업 — 자체 로컬 트랜잭션(legacyTransactionManager).
 */
@Service
class LegacyMemberService(
    private val legacyRepo: LegacyMemberRepository,
) {
    /** Step 2 정방향: 기존 회원 INSERT. */
    @Transactional("legacyTransactionManager")
    fun create(email: String, name: String) {
        legacyRepo.save(LegacyMember(email = email, name = name))
    }

    /** Saga 시뮬레이션용: 의도적으로 실패해 보상을 유도. */
    @Transactional("legacyTransactionManager")
    fun createThenFail(email: String, name: String) {
        legacyRepo.save(LegacyMember(email = email, name = name))
        throw IllegalStateException("Saga: legacy DB 저장 실패 시뮬레이션 → Step 1 보상(DELETE) 유도")
    }
}

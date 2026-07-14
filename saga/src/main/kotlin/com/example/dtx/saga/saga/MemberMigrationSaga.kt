package com.example.dtx.saga.saga

import com.example.dtx.saga.service.LegacyMemberService
import com.example.dtx.saga.service.NewMemberService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Saga Orchestrator — 회원 마이그레이션 시나리오.
 *
 * 단계(각각 독립된 로컬 트랜잭션):
 *  Step 1. 신규 DB INSERT   (newTransactionManager)
 *  Step 2. 기존 DB INSERT   (legacyTransactionManager)
 *
 * Step 2 가 실패하면 → Step 1 보상 트랜잭션(신규 DB DELETE) 을 실행해 이전 상태로 되돌린다.
 *  - 2PC 처럼 전역 잠금을 잡지 않아 블로킹이 없다.
 *  - 단, 일시적으로 신규 DB 에만 데이터가 있는 "중간 상태"가 노출된다(최종 일관성, AP).
 */
@Service
class MemberMigrationSaga(
    private val newSvc: NewMemberService,
    private val legacySvc: LegacyMemberService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createMember(email: String, name: String): SagaResult {
        val newId = newSvc.create(email, name)        // Step 1
        log.info("[SAGA] Step 1 신규 DB INSERT 완료 id={} email={}", newId, email)
        try {
            legacySvc.create(email, name)             // Step 2
            log.info("[SAGA] Step 2 기존 DB INSERT 완료 email={}", email)
            return SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] Step 2 실패 → 보상 트랜잭션 실행: 신규 DB DELETE id={} cause={}", newId, e.message)
            newSvc.compensateDelete(newId)            // 보상(Step 1 되돌림)
            return SagaResult.COMPENSATED
        }
    }

    /** 보상 경로 시뮬레이션: Step 2 를 의도적으로 실패. */
    fun createMemberThenFail(email: String, name: String): SagaResult {
        val newId = newSvc.create(email, name)
        log.info("[SAGA] Step 1 신규 DB INSERT 완료 id={} email={}", newId, email)
        try {
            legacySvc.createThenFail(email, name)
            return SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] 보상 실행: 신규 DB DELETE id={}", newId)
            newSvc.compensateDelete(newId)
            return SagaResult.COMPENSATED
        }
    }

    enum class SagaResult { COMPLETED, COMPENSATED }
}

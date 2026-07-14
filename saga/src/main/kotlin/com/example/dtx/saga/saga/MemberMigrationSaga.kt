package com.example.dtx.saga.saga

import com.example.dtx.saga.service.ContactService
import com.example.dtx.saga.service.ProfileService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Saga Orchestrator — 회원 분할 저장 시나리오.
 *
 * 레거시 단일 회원(컬럼 다수) → 신규 DB 2개(profile/contact) 분할 저장.
 * 단계(각각 독립된 로컬 트랜잭션):
 *  Step 1. profile DB INSERT   (profileTransactionManager)
 *  Step 2. contact DB INSERT   (contactTransactionManager)
 *
 * Step 2 가 실패하면 → Step 1 보상 트랜잭션(profile DB DELETE) 으로 되돌린다.
 *  - 블로킹 없이 보상 가능 (2PC 와 달리)
 *  - 단, 일시적으로 profile DB 에만 데이터가 있는 "중간 상태" 노출 (최종 일관성, AP)
 */
@Service
class MemberMigrationSaga(
    private val profileSvc: ProfileService,
    private val contactSvc: ContactService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun registerMember(name: String, email: String, phone: String): SagaResult {
        val profileId = profileSvc.create(name)        // Step 1
        log.info("[SAGA] Step 1 profile DB INSERT 완료 id={} name={}", profileId, name)
        try {
            contactSvc.create(profileId, email, phone) // Step 2
            log.info("[SAGA] Step 2 contact DB INSERT 완료 profileId={}", profileId)
            return SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] Step 2 실패 → 보상: profile DB DELETE id={} cause={}", profileId, e.message)
            profileSvc.compensateDelete(profileId)     // 보상(Step 1 되돌림)
            return SagaResult.COMPENSATED
        }
    }

    /** 보상 경로 시뮬레이션: Step 2 를 의도적으로 실패. */
    fun registerMemberThenFail(name: String, email: String, phone: String): SagaResult {
        val profileId = profileSvc.create(name)
        log.info("[SAGA] Step 1 profile DB INSERT 완료 id={} name={}", profileId, name)
        try {
            contactSvc.createThenFail(profileId, email, phone)
            return SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] 보상 실행: profile DB DELETE id={}", profileId)
            profileSvc.compensateDelete(profileId)
            return SagaResult.COMPENSATED
        }
    }

    enum class SagaResult { COMPLETED, COMPENSATED }
}

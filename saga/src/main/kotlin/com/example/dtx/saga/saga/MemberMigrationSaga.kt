package com.example.dtx.saga.saga

import com.example.dtx.saga.service.ContactService
import com.example.dtx.saga.service.ProfileService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Saga Orchestrator — 회원 분할 저장/수정 시나리오.
 *
 * ① INSERT 시나리오: registerMember
 *   Step1(profile INSERT) → Step2(contact INSERT); 실패 시 Step1 보상(profile DELETE)
 *
 * ② UPDATE 시나리오: updateMemberNameAndEmail   ← ★ 핵심
 *   Step1(profile UPDATE name) → before 이미지 보관
 *   Step2(contact UPDATE email); 실패 시 Step1 보상 = before 이미지로 재 UPDATE
 *   (UPDATE 보상은 DELETE 로 안 됨 → "이전 값으로 다시 UPDATE")
 */
@Service
class MemberMigrationSaga(
    private val profileSvc: ProfileService,
    private val contactSvc: ContactService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ─────────────────────────────────────────────────────────────
    // ① INSERT 시나리오 + 보상(DELETE)
    // ─────────────────────────────────────────────────────────────
    fun registerMember(name: String, email: String, phone: String): SagaResult {
        val profileId = profileSvc.create(name)        // Step 1 INSERT
        log.info("[SAGA] Step 1 profile INSERT 완료 id={} name={}", profileId, name)
        return try {
            contactSvc.create(profileId, email, phone) // Step 2 INSERT
            log.info("[SAGA] Step 2 contact INSERT 완료 profileId={}", profileId)
            SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] Step 2 실패 → INSERT 보상(profile DELETE) id={} cause={}", profileId, e.message)
            profileSvc.compensateDelete(profileId)
            SagaResult.COMPENSATED
        }
    }

    fun registerMemberThenFail(name: String, email: String, phone: String): SagaResult {
        val profileId = profileSvc.create(name)
        log.info("[SAGA] Step 1 profile INSERT 완료 id={} name={}", profileId, name)
        return try {
            contactSvc.createThenFail(profileId, email, phone)
            SagaResult.COMPLETED
        } catch (e: Exception) {
            log.warn("[SAGA] INSERT 보상(profile DELETE) id={}", profileId)
            profileSvc.compensateDelete(profileId)
            SagaResult.COMPENSATED
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ② UPDATE 시나리오 + 보상(before 이미지로 재 UPDATE) ★
    // ─────────────────────────────────────────────────────────────
    fun updateMemberNameAndEmail(
        profileId: Long,
        newName: String,
        newEmail: String,
        failContact: Boolean = false,
    ): SagaResult {
        // Step 1 UPDATE: profile name 변경 + before 이미지 보관
        val beforeName = profileSvc.updateName(profileId, newName)
        log.info("[SAGA] Step 1 profile UPDATE 완료 id={} {}→{} (before={} 보관)", profileId, beforeName, newName, beforeName)
        return try {
            // Step 2 UPDATE: contact email 변경
            if (failContact) contactSvc.updateEmailThenFail(profileId, newEmail)
            else contactSvc.updateEmail(profileId, newEmail)
            log.info("[SAGA] Step 2 contact UPDATE 완료 profileId={}", profileId)
            SagaResult.COMPLETED
        } catch (e: Exception) {
            // UPDATE 보상: before 이미지로 재 UPDATE (DELETE 로는 안 됨)
            log.warn("[SAGA] Step 2 실패 → UPDATE 보상(profile name {}→{}) cause={}", newName, beforeName, e.message)
            profileSvc.compensateUpdateName(profileId, beforeName)
            SagaResult.COMPENSATED
        }
    }

    enum class SagaResult { COMPLETED, COMPENSATED }
}

package com.example.dtx.saga.service

import com.example.dtx.saga.domain.contact.MemberContact
import com.example.dtx.saga.repository.contact.MemberContactRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 신규 DB B(contact) 작업 — 자체 로컬 TX(contactTransactionManager). */
@Service
class ContactService(
    private val contactRepo: MemberContactRepository,
) {
    /** Step 2 정방향: contact INSERT. */
    @Transactional("contactTransactionManager")
    fun create(profileId: Long, email: String, phone: String) {
        contactRepo.save(MemberContact(profileId = profileId, email = email, phone = phone))
    }

    /** Saga 시뮬레이션용: 의도적으로 실패해 보상을 유도. */
    @Transactional("contactTransactionManager")
    fun createThenFail(profileId: Long, email: String, phone: String) {
        contactRepo.save(MemberContact(profileId = profileId, email = email, phone = phone))
        throw IllegalStateException("Saga: contact DB 저장 실패 시뮬레이션 → Step 1 보상(DELETE) 유도")
    }
}

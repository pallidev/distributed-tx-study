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
    /** Step 2 정방향(INSERT): contact 저장. */
    @Transactional("contactTransactionManager")
    fun create(profileId: Long, email: String, phone: String) {
        contactRepo.save(MemberContact(profileId = profileId, email = email, phone = phone))
    }

    /** INSERT 시뮬레이션: 의도적 실패 → INSERT 보상(DELETE) 유도. */
    @Transactional("contactTransactionManager")
    fun createThenFail(profileId: Long, email: String, phone: String) {
        contactRepo.save(MemberContact(profileId = profileId, email = email, phone = phone))
        throw IllegalStateException("Saga: contact INSERT 실패 시뮬 → Step 1 INSERT 보상(DELETE) 유도")
    }

    /** Step 2 정방향(UPDATE): contact email 변경. */
    @Transactional("contactTransactionManager")
    fun updateEmail(profileId: Long, newEmail: String) {
        val contact = contactRepo.findByProfileId(profileId)
            ?: throw IllegalStateException("contact not found: $profileId")
        contact.email = newEmail
    }

    /** UPDATE 시뮬레이션: 의도적 실패 → Step 1 UPDATE 보상(before 이미지 재UPDATE) 유도. */
    @Transactional("contactTransactionManager")
    fun updateEmailThenFail(profileId: Long, newEmail: String) {
        val contact = contactRepo.findByProfileId(profileId)
            ?: throw IllegalStateException("contact not found: $profileId")
        contact.email = newEmail
        throw IllegalStateException("Saga: contact UPDATE 실패 시뮬 → Step 1 UPDATE 보상(before 이미지 재UPDATE) 유도")
    }
}

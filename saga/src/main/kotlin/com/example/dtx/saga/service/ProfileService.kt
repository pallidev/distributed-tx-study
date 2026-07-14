package com.example.dtx.saga.service

import com.example.dtx.saga.domain.profile.MemberProfile
import com.example.dtx.saga.repository.profile.MemberProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 신규 DB A(profile) 작업 — 자체 로컬 TX(profileTransactionManager). */
@Service
class ProfileService(
    private val profileRepo: MemberProfileRepository,
) {
    /** Step 1 정방향: profile INSERT. id 반환(보상에 사용). */
    @Transactional("profileTransactionManager")
    fun create(name: String): Long =
        profileRepo.save(MemberProfile(name = name)).id!!

    /** Step 1 보상(compensating): profile DELETE. INSERT 보상 = DELETE. */
    @Transactional("profileTransactionManager")
    fun compensateDelete(id: Long) {
        profileRepo.deleteById(id)
    }
}

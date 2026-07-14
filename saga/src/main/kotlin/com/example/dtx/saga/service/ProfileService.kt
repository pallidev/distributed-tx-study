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
    /** Step 1 정방향(INSERT): profile 저장. id 반환. */
    @Transactional("profileTransactionManager")
    fun create(name: String): Long =
        profileRepo.save(MemberProfile(name = name)).id!!

    /** INSERT 보상: profile DELETE. */
    @Transactional("profileTransactionManager")
    fun compensateDelete(id: Long) {
        profileRepo.deleteById(id)
    }

    /**
     * Step 1 정방향(UPDATE): profile name 변경.
     * ★ 보상을 위해 "변경 전 이름(before image)"을 반환한다. (DELETE 로는 되돌릴 수 없음)
     */
    @Transactional("profileTransactionManager")
    fun updateName(id: Long, newName: String): String {
        val profile = profileRepo.findById(id).orElseThrow()
        val beforeName = profile.name
        profile.name = newName
        return beforeName
    }

    /**
     * UPDATE 보상(compensating): before 이미지로 재 UPDATE 해 원래 값으로 되돌린다.
     * (INSERT 보상=DELETE 와 달리, UPDATE 보상은 "이전 값으로 다시 UPDATE" 해야 함)
     */
    @Transactional("profileTransactionManager")
    fun compensateUpdateName(id: Long, beforeName: String) {
        val profile = profileRepo.findById(id).orElseThrow()
        profile.name = beforeName
    }
}

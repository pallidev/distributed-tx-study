package com.example.dtx.saga.service

import com.example.dtx.saga.domain.NewMember
import com.example.dtx.saga.repository.NewMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 신규(new) DB 작업 — 자체 로컬 트랜잭션(newTransactionManager).
 * Saga 단계의 정방향(INSERT) 과 보상(DELETE) 을 함께 제공.
 */
@Service
class NewMemberService(
    private val newRepo: NewMemberRepository,
) {
    /** Step 1 정방향: 신규 회원 INSERT. 생성된 id 반환(보상에 사용). */
    @Transactional("newTransactionManager")
    fun create(email: String, name: String): Long =
        newRepo.save(NewMember(email = email, name = name)).id!!

    /** Step 1 보상(compensating): 신규 회원 DELETE. INSERT 보상 = DELETE. */
    @Transactional("newTransactionManager")
    fun compensateDelete(id: Long) {
        newRepo.deleteById(id)
    }
}

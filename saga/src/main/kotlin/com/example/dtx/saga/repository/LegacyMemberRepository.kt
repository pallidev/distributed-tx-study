package com.example.dtx.saga.repository

import com.example.dtx.saga.domain.LegacyMember
import org.springframework.data.jpa.repository.JpaRepository

interface LegacyMemberRepository : JpaRepository<LegacyMember, Long> {
    fun findByEmail(email: String): LegacyMember?
}

package com.example.dtx.twopc.repository

import com.example.dtx.twopc.domain.LegacyMember
import org.springframework.data.jpa.repository.JpaRepository

interface LegacyMemberRepository : JpaRepository<LegacyMember, Long> {
    fun findByEmail(email: String): LegacyMember?
}

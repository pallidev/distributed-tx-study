package com.example.dtx.twopc.repository

import com.example.dtx.twopc.domain.NewMember
import org.springframework.data.jpa.repository.JpaRepository

interface NewMemberRepository : JpaRepository<NewMember, Long> {
    fun findByEmail(email: String): NewMember?
}

package com.example.dtx.saga.repository

import com.example.dtx.saga.domain.NewMember
import org.springframework.data.jpa.repository.JpaRepository

interface NewMemberRepository : JpaRepository<NewMember, Long> {
    fun findByEmail(email: String): NewMember?
}

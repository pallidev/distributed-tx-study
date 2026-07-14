package com.example.dtx.saga.repository.contact

import com.example.dtx.saga.domain.contact.MemberContact
import org.springframework.data.jpa.repository.JpaRepository

interface MemberContactRepository : JpaRepository<MemberContact, Long> {
    fun findByProfileId(profileId: Long): MemberContact?
}

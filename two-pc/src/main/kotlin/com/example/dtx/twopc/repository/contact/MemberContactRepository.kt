package com.example.dtx.twopc.repository.contact

import com.example.dtx.twopc.domain.contact.MemberContact
import org.springframework.data.jpa.repository.JpaRepository

interface MemberContactRepository : JpaRepository<MemberContact, Long> {
    fun findByProfileId(profileId: Long): MemberContact?
}

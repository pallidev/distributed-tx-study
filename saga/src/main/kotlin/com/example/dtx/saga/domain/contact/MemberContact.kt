package com.example.dtx.saga.domain.contact

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "member_contact")
class MemberContact(
    @Id @GeneratedValue
    var id: Long? = null,
    @Column(name = "profile_id")
    var profileId: Long,
    var email: String,
    var phone: String,
)

package com.example.dtx.saga.domain.profile

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "member_profile")
class MemberProfile(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String,
)

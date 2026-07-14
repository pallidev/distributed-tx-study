package com.example.dtx.twopc.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 기존(legacy) DB 의 회원 엔티티. */
@Entity
@Table(name = "member")
class LegacyMember(
    @Id @GeneratedValue
    var id: Long? = null,
    var email: String,
    var name: String,
)

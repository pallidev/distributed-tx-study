package com.example.dtx.twopc.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 신규(new) DB 의 회원 엔티티. 스키마는 legacy 와 동일. */
@Entity
@Table(name = "member")
class NewMember(
    @Id @GeneratedValue
    var id: Long? = null,
    var email: String,
    var name: String,
)

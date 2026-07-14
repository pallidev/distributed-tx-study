package com.example.dtx.twopc.domain.contact

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 신규 DB B — 회원 연락처/계정.
 * (레거시 단일 회원 테이블의 "연락처" 영역을 분리. MemberProfile.id 를 참조)
 */
@Entity
@Table(name = "member_contact")
class MemberContact(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "profile_id")
    var profileId: Long,
    var email: String,
    var phone: String,
)

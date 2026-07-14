package com.example.dtx.twopc.domain.profile

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 신규 DB A — 회원 기본정보.
 * (레거시 단일 회원 테이블의 컬럼 중 "기본정보" 영역을 분리한 도메인)
 */
@Entity
@Table(name = "member_profile")
class MemberProfile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String,
)

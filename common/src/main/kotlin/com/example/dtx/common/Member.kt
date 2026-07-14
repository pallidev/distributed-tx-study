package com.example.dtx.common

/**
 * 회원 도메인 — 마이그레이션 시나리오의 핵심.
 *
 * 시나리오: 기존(legacy) DB ↔ 신규(new) DB 양방향 동기화.
 *  - 2PC 모듈: 양쪽 DB 에 한 트랜잭션으로 원자적 쓰기 (강한 일관성, CP)
 *  - Saga 모듈: 각 DB 에 로컬 트랜잭션으로 쓰고, 실패 시 보상 (최종 일관성, AP)
 */
data class Member(
    val id: Long? = null,
    val email: String,
    val name: String,
)

/**
 * Saga 보상 트랜잭션용 "변경 전 이미지(before image)".
 * - UPDATE 보상 = before 스냅숏으로 되돌리는 재 UPDATE
 * - CDC 메시지의 before 필드와 같은 역할.
 */
data class MemberSnapshot(
    val email: String,
    val name: String,
)

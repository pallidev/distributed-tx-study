package com.example.dtx.saga

import com.example.dtx.saga.domain.legacy.LegacyMember
import com.example.dtx.saga.domain.newdb.NewMember
import com.example.dtx.saga.repository.legacy.LegacyMemberRepository
import com.example.dtx.saga.repository.newdb.NewMemberRepository
import com.example.dtx.saga.saga.MemberMigrationSaga
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Saga 시나리오 검증
 *  1. 정상: Step1(신규) + Step2(기존) 모두 저장
 *  2. 보상: Step2 실패 → Step1 보상(신규 DELETE). 결과적으로 양쪽 모두 해당 데이터 없음
 */
@SpringBootTest
class MemberMigrationSagaTest {

    @Autowired private lateinit var saga: MemberMigrationSaga
    @Autowired private lateinit var newRepo: NewMemberRepository
    @Autowired private lateinit var legacyRepo: LegacyMemberRepository

    @Test
    fun `정상 흐름 - 양쪽 DB 모두 저장된다`() {
        val email = "ok-${System.nanoTime()}@example.com"
        val result = saga.createMember(email, "정상")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPLETED)
        assertThat(newRepo.findAll().map(NewMember::email)).contains(email)
        assertThat(legacyRepo.findAll().map(LegacyMember::email)).contains(email)
    }

    @Test
    fun `보상 흐름 - Step2 실패시 Step1 이 보상(DELETE) 되어 양쪽 모두 데이터가 없다`() {
        val email = "fail-${System.nanoTime()}@example.com"
        val result = saga.createMemberThenFail(email, "실패")

        assertThat(result).isEqualTo(MemberMigrationSaga.SagaResult.COMPENSATED)
        // 보상(신규 DB DELETE) + Step2 실패(기존 미저장) → 양쪽 모두 없어야
        assertThat(newRepo.findAll().map(NewMember::email)).doesNotContain(email)
        assertThat(legacyRepo.findAll().map(LegacyMember::email)).doesNotContain(email)
    }
}

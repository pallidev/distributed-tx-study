# distributed-tx-study

> **2PC → Saga 마이그레이션 학습 프로젝트**
> 실제 회원 시스템 무중단 마이그레이션에서 겪은 **2PC(JTA/Atomikos)** 의 한계를 코드로 재현하고,
> 같은 시나리오를 **Saga(Orchestration)** 로 다시 설계해 비교한다.

Spring Boot 4 · Kotlin · JPA. **별도 DB 설치 없이** `./gradlew test` (Testcontainers MySQL XA / H2) 또는 `./gradlew bootRun` (H2) 로 실행·검증.

---

## 시나리오 — 레거시 단일 회원 → 신규 DB 2개 분할

```
레거시 단일 회원 테이블 (컬럼 다수)
        │  도메인별 분해
        ▼
신규 DB A (profile)   ←─ 2PC/Saga 로 원자적 분할 저장 ─→  신규 DB B (contact)
 member_profile                                            member_contact
 (id, name)                                                (id, profile_id, email, phone)
```

회원 1건을 **신규 DB 2개(profile/contact)로 분할해 저장**할 때, 양쪽 쓰기의 원자성을 어떻게 보장할 것인가 → **2PC** vs **Saga** 두 접근을 같은 시나리오로 구현.

> (기존 레거시 DB → 신규 DB 동기화는 CDC/DMS가 담당하고, 이 프로젝트는 **신규 DB 2개 사이의 정합성**에 집중한다.)

---

## 모듈 구조

```
distributed-tx-study/
├── common/      # 공통 도메인 모델 (Member, 보상용 Snapshot)
├── two-pc/      # 2PC: JTA(Atomikos)로 profile·contact 두 DB 원자적 쓰기 (강한 일관성, CP)
└── saga/        # Saga: 각 DB 로컬 TX + 보상 트랜잭션 (최종 일관성, AP)
```

---

## ✅ 검증 (테스트로 시나리오 확인)

```bash
./gradlew test                          # 두 모듈 시나리오 전체
./gradlew :two-pc:test                  # 2PC (Testcontainers MySQL XA)
./gradlew :saga:test                    # Saga (H2)
```

| 모듈 | 시나리오 | 기대 | 검증 |
|------|----------|------|------|
| **two-pc** | 정상 `registerMember` | profile·contact **두 DB 모두 커밋** | `MemberMigrationServiceTest > 정상 흐름` ✅ |
| **two-pc** | `registerMemberThenFail` (contact 저장 직전 예외) | 두 DB 모두 롤백(데이터 없음) | `> 롤백 흐름` ✅ |
| **saga** | `registerMember` | Step1(profile)+Step2(contact) 완료 → COMPLETED | `MemberMigrationSagaTest > 정상 흐름` ✅ |
| **saga** | `registerMemberThenFail` | Step2 실패 → Step1 보상(profile DELETE) → COMPENSATED, 양쪽 없음 | `> 보상 흐름` ✅ |
| **saga** | `updateMemberNameAndEmail`(fail) | **UPDATE** Step1(profile name)+Step2(contact email), 실패 시 **before 이미지로 재 UPDATE** | `> UPDATE 보상` ✅ |

> two-pc 테스트는 **실제 MySQL XA**(Testcontainers 컨테이너 2개: profile / contact)로 Atomikos 가 동작한다.

---

## 📚 학습 경로 (테스트를 이 순서로 읽으면 단계적 학습)

1. **`two-pc/MemberMigrationServiceTest`** (JUnit5 + Testcontainers MySQL XA)
   - `정상 흐름` → 2PC 로 양쪽 DB 원자적 **커밋**
   - `롤백 흐름` → 비즈니스 예외 시 양쪽 DB **롤백**
2. **`saga/MemberMigrationSagaSpec`** (Kotest BehaviorSpec, Given/When/Then)
   - `① INSERT 정상` → Step1(profile)+Step2(contact) **COMPLETED**
   - `② INSERT 보상` → Step2 실패 → Step1 **보상(DELETE)**
   - `③ UPDATE 보상` → Step2 실패 → Step1 **before 이미지로 재 UPDATE**

## 🧪 테스트 도구 — 상황에 따라 분리

| 모듈 | 도구 | 이유 |
|------|------|------|
| **saga** | **Kotest BehaviorSpec** | 학습 시나리오를 `Given/When/Then` 로 표현하기 좋음 (H2라 단순) |
| **two-pc** | **JUnit5 + Testcontainers** (`@DynamicPropertySource`) | 다중 DataSource(XA) URL 을 컨테이너 시작 시점에 주입해야 해서 `@DynamicPropertySource` 가 깔끔. (Kotest + Testcontainers + Spring 동적 프로퍼티 조합은 설정이 과도함) |

> "상황에 따라 도구를 선택한다"는 것 자체가 실무 감각 — Kotest가 학습 시나리오 표현엔 좋지만, 인프라(Testcontainers) 주입이 핵심인 테스트는 JUnit5 `@DynamicPropertySource`가 더 적합.

---

## 2PC vs Saga 비교

| 구분 | **2PC** (two-pc) | **Saga** (saga) |
|------|------------------|-----------------|
| 일관성 | 강한 일관성 (Strong) | 최종 일관성 (Eventual) |
| 잠금 | 전역 리소스 잠금 (블로킹) | 없음 (각 단계 독립) |
| 실패 복구 | Phase 2 실패 시 **롤백 불가 → 자동 재시도(블로킹)** | **보상 트랜잭션**(profile INSERT→DELETE) 으로 명확히 되돌림 |
| 코디네이터 | SPOF (Atomikos) | 분산 (Orchestrator) |
| 중간 상태 | 없음 (원자적) | 일시적 불일치 허용 (profile만 있는 구간) |
| CAP | CP | AP |
| 적합 | 강한 일관성 필수(계좌) | 가용성·장애 격리 중요(회원 동기화) |

---

## 학습 포인트

### two-pc (`MemberMigrationService`)
- `@Transactional`(JTA) 하나로 `profileRepo.save` + `contactRepo.save` 가 **한 분산 트랜잭션**에 묶임
- `registerMemberThenFail` → contact 저장 직전 예외 → **두 DB 모두 롤백** (테스트로 검증)
- 핵심 한계: Phase 2 커밋 단계에서 한쪽이 이미 커밋된 뒤 다른 쪽이 실패하면 **되돌릴 수 없고**, Atomikos 는 실패한 쪽에 COMMIT 을 **자동 재시도(블로킹)** 한다 → 실제 운영에서 체감한 한계

### saga (`MemberMigrationSaga`)
- 각 단계가 **독립된 로컬 트랜잭션**(`profileTransactionManager` / `contactTransactionManager`)
- `Step 1(profile INSERT) → Step 2(contact INSERT)`; Step 2 실패 시 **Step 1 보상(profile DELETE)** (테스트로 검증)
- **UPDATE 보상** (`updateMemberNameAndEmail`): `profile name UPDATE`(before 이미지 보관) → `contact email UPDATE`, 실패 시 **before 이미지로 재 UPDATE** (DELETE 로는 UPDATE 를 되돌릴 수 없음) (테스트로 검증)
- 블로킹 없이 **보상 가능** → 일시적 중간 상태(profile DB 에만 데이터)를 감수하는 대신 가용성·장애 격리 확보

### 보상 트랜잭션 종류 (★ 핵심)
| 정방향 | 보상 | 비고 |
|--------|------|------|
| **INSERT** | **DELETE** | 생성된 row 제거 (단순) |
| **UPDATE** | **before 이미지로 재 UPDATE** | 변경 전 값을 보관해두고 그 값으로 되돌림 (DELETE 불가) |
| **DELETE** | (보상 불가) | 취소 불가능한 연산 → 가장 마지막 단계에 배치해야 |

---

## 🧗 Lessons Learned (직접 부딪힌 함정)

Spring Boot 4 + Hibernate 7 + Atomikos 6 + MySQL XA 매트릭스에서 **2PC 테스트가 동작하기까지** 만난 문제와 해결:

### 1. Hibernate 가 JTA 글로벌 TX 에 참여하지 않는 문제 (insert 자체가 안 됨)
- **원인**: 다중 DataSource라 **Boot JPA auto-config가 `EntityManagerFactoryBuilder` 빈을 생성하지 않음**(백오프). `LocalContainerEntityManagerFactoryBean` 직접 생성으로 대체하면 `.jta(true)` 가 빠져 Hibernate 가 JTA TX 에 참여하지 못함 → persist/flush/insert 가 아예 발생 X
- **해결**: `EntityManagerFactoryBuilder` 를 **`@Bean`으로 직접 생성**하고 `.jta(true)` 로 EMF 빌드 (`JtaConfig.entityManagerFactoryBuilder`)

### 2. MySQL XA에서 커넥션이 글로벌 TX 에 enlist 안 되는 문제
- **원인**: Atomikos 로그에 `NotInBranchStateHandler` (XA branch enlist 안 됨)
- **해결**: MySQL Connector/J XA 공식 옵션 **`pinGlobalTxToPhysicalConnection=true`** (같은 XID → 같은 physical connection 라우팅)

### 3. Hibernate 자동 DDL(hbm2ddl) ↔ JTA 글로벌 TX 충돌
- **원인**: MySQL XA는 "global transaction running 중 commit 호출 금지" → Hibernate hbm2ddl(auto DDL)이 JTA TX 안에서 commit 시도 → 거부
- **해결**: 테스트에선 `hibernate.hbm2ddl.auto=none` + `@BeforeAll` 로 직접 DDL(`CREATE TABLE`) (Boot4+Hibernate7+JTA 조합의 함정)

### 4. Spring Boot 4 변경
- `spring-boot-starter-jta-atomikos` 스타터가 **Boot 4에서 제거됨** → Atomikos 공식 **`transactions-spring-boot4:6.0.1`** 사용
- `EntityManagerFactoryBuilder` 패키지가 Boot 3 `org.springframework.boot.orm.jpa` → Boot 4 `org.springframework.boot.jpa` 로 이동

> Phase 2 커밋 단계 실패(한쪽 DB commit 실패) → Atomikos 자동 재시도(`oltp_max_retries=5`, 로그에서 확인)는 실제 컨테이너 장애 주입이 필요해 단위 테스트에선 다루지 않는다. 운영에서 겪은 한계는 별도 문서로.

---

## 왜 만들었나

실제 회원 시스템 무중단 마이그레이션에서 레거시 단일 회원 테이블(컬럼 다수)을 **신규 DB 2개(profile/contact)로 도메인 분할**하면서, 두 DB 정합성을 위해 **2PC(JTA/Atomikos)를 적용**했다.
운영하며 **2PC의 치명적 한계**(Phase 2 실패 시 롤백 불가·블로킹·코디네이터 SPOF)를 체감했고, 이후 **보상 트랜잭션 기반 Saga 패턴을 학습**했다.

이 저장소는 그 학습을 **코드로 직접 구현·검증**하기 위한 것으로, 두 패턴의 트레이드오프를 테스트로 재현 가능한 형태로 남긴다.

# distributed-tx-study

> **2PC → Saga 마이그레이션 학습 프로젝트**
> 실제 회원 시스템 무중단 마이그레이션에서 겪은 **2PC(JTA/Atomikos)** 의 한계를 코드로 재현하고,
> 같은 시나리오를 **Saga(Orchestration)** 로 다시 설계해 비교한다.

Spring Boot 4 · Kotlin · JPA · H2(XA). 별도 DB 설치 없이 `./gradlew` 로 실행 가능.

---

## 시나리오 — 회원 마이그레이션

```
기존(legacy) DB  ⟷  신규(new) DB   양방향 동기화
   member              member
```

회원 1건을 **양쪽 DB에 동기화**해야 하는데, 서비스 중단 없이 진행해야 한다. 이 "양쪽 쓰기의 원자성"을 어떻게 보장할 것인가 → **2PC** vs **Saga** 두 접근을 같은 시나리오로 구현.

---

## 모듈 구조

```
distributed-tx-study/
├── common/      # 공통 도메인(Member, 보상용 Snapshot)
├── two-pc/      # 2PC: JTA(Atomikos)로 양쪽 DB 원자적 쓰기 (강한 일관성, CP)
└── saga/        # Saga: 각 DB 로컬 TX + 보상 트랜잭션 (최종 일관성, AP)
```

---

## 실행

```bash
# 2PC — 양쪽 DB를 한 트랜잭션으로 동기화
./gradlew :two-pc:bootRun

# Saga — 각 DB 로컬 TX + 실패 시 보상(DELETE)
./gradlew :saga:bootRun
```

두 앱 모두 시작 시 데모(`alice` 정상 / `bob` 실패→보상)를 자동 실행하고 로그로 흐름을 보여준다.

---

## 2PC vs Saga 비교

| 구분 | **2PC** (two-pc) | **Saga** (saga) |
|------|------------------|-----------------|
| 일관성 | 강한 일관성 (Strong) | 최종 일관성 (Eventual) |
| 잠금 | 전역 리소스 잠금 (블로킹) | 없음 (각 단계 독립) |
| 실패 복구 | Phase 2 실패 시 **롤백 불가 → 자동 재시도(블로킹)** | **보상 트랜잭션**(INSERT→DELETE) 으로 명확히 되돌림 |
| 코디네이터 | SPOF (Atomikos) | 분산 (Orchestrator) |
| 중간 상태 | 없음 (원자적) | 일시적 불일치 허용 |
| CAP | CP | AP |
| 적합 | 강한 일관성 필수(계좌) | 가용성·장애 격리 중요(회원 동기화) |

---

## 학습 포인트

### two-pc (`MemberMigrationService`)
- `@Transactional`(JTA) 하나로 `newRepo.save` + `legacyRepo.save` 가 **한 분산 트랜잭션**에 묶임
- `createMemberThenFail` → legacy 저장 직전 예외 → **양쪽 롤백** 확인
- 핵심 한계: Phase 2 커밋 단계에서 한쪽이 이미 커밋된 뒤 다른 쪽이 실패하면 **되돌릴 수 없고**, Atomikos 는 실패한 쪽에 COMMIT 을 **자동 재시도(블로킹)** 한다 → 실제 운영에서 체감한 한계

### saga (`MemberMigrationSaga`)
- 각 단계가 **독립된 로컬 트랜잭션**(`legacyTransactionManager` / `newTransactionManager`)
- `Step 1(신규 INSERT) → Step 2(기존 INSERT)`; Step 2 실패 시 **Step 1 보상(DELETE)**
- 블로킹 없이 **보상 가능** → 일시적 중간 상태(신규 DB 에만 데이터)를 감수하는 대신 가용성·장애 격리 확보

---

## 왜 만들었나

실제 회원 시스템 무중단 마이그레이션에서 **2PC(JTA/Atomikos)를 적용**해 데이터 정합성을 맞았다.
운영하며 **2PC의 치명적 한계**(Phase 2 실패 시 롤백 불가·블로킹·코디네이터 SPOF)를 체감했고,
이후 **보상 트랜잭션 기반 Saga 패턴을 학습**했다.

이 저장소는 그 학습을 **코드로 직접 구현/비교**하기 위한 것으로, 두 패턴의 트레이드오프를 재현 가능한 형태로 남긴다.

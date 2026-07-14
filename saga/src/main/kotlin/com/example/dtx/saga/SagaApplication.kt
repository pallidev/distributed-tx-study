package com.example.dtx.saga

import com.example.dtx.saga.saga.MemberMigrationSaga
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@SpringBootApplication
class SagaApplication

fun main(args: Array<String>) {
    runApplication<SagaApplication>(*args)
}

@Component
class SagaDemoRunner(private val saga: MemberMigrationSaga) {
    @EventListener(ApplicationReadyEvent::class)
    fun run() {
        val ok = saga.createMember("alice@example.com", "앨리스")
        println("[SAGA] alice 결과 = $ok")

        val compensated = saga.createMemberThenFail("bob@example.com", "밥")
        println("[SAGA] bob 결과(보상 예상) = $compensated")
    }
}

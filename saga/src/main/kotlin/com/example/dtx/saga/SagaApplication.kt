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
        val ok = saga.registerMember("앨리스", "alice@example.com", "010-1111-2222")
        println("[SAGA] alice 결과 = $ok")

        val compensated = saga.registerMemberThenFail("밥", "bob@example.com", "010-9999-9999")
        println("[SAGA] bob 결과(보상 예상) = $compensated")
    }
}

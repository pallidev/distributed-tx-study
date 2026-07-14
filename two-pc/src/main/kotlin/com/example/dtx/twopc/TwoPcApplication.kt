package com.example.dtx.twopc

import com.example.dtx.twopc.service.MemberMigrationService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@SpringBootApplication
class TwoPcApplication

fun main(args: Array<String>) {
    runApplication<TwoPcApplication>(*args)
}

@Component
class TwoPcDemoRunner(private val svc: MemberMigrationService) {
    @EventListener(ApplicationReadyEvent::class)
    fun run() {
        svc.registerMember("앨리스", "alice@example.com", "010-1111-2222")
        println("[2PC] alice 분할 저장 완료 (profile+contact)")
    }
}

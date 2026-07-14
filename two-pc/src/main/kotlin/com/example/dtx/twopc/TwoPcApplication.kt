package com.example.dtx.twopc

import com.example.dtx.twopc.service.MemberMigrationService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
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
        svc.createMember("alice@example.com", "앨리스")
        println("[2PC] alice 동기화 완료")
    }
}

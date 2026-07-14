package com.example.dtx.saga

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringTestExtension

/** 모든 Kotest spec 에 SpringTestExtension 을 전역 적용 → 생성자 주입(@SpringBootTest) 지원. */
class ProjectConfig : AbstractProjectConfig() {
    override fun extensions() = listOf(SpringTestExtension())
}

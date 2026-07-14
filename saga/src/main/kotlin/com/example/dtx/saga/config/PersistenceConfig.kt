package com.example.dtx.saga.config

import com.example.dtx.saga.domain.LegacyMember
import com.example.dtx.saga.domain.NewMember
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Saga 는 JTA(2PC) 없이 각 DB 마다 독립적인 로컬 트랜잭션 매니저를 둔다.
 *  - legacyTransactionManager / newTransactionManager 각각 자체 커넥션·커밋
 *  - Orchestrator(MemberMigrationSaga) 가 단계별로 호출하고, 실패 시 보상 트랜잭션을 직접 호출한다.
 */
@Configuration
class PersistenceConfig {

    private fun h2(url: String): DataSource = DriverManagerDataSource().apply {
        setDriverClassName("org.h2.Driver")
        setUrl(url)
        username = "sa"
        password = ""
    }

    @Bean(name = ["legacyDataSource"])
    @Primary
    fun legacyDataSource(): DataSource = h2("jdbc:h2:mem:legacy;DB_CLOSE_DELAY=-1;MODE=MySQL")

    @Bean(name = ["newDataSource"])
    fun newDataSource(): DataSource = h2("jdbc:h2:mem:newdb;DB_CLOSE_DELAY=-1;MODE=MySQL")

    @Bean(name = ["legacyEntityManagerFactory"])
    @Primary
    fun legacyEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder.dataSource(legacyDataSource()).packages(LegacyMember::class.java).persistenceUnit("legacy").build()

    @Bean(name = ["newEntityManagerFactory"])
    fun newEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder.dataSource(newDataSource()).packages(NewMember::class.java).persistenceUnit("new").build()

    @Bean(name = ["legacyTransactionManager"])
    @Primary
    fun legacyTransactionManager(
        @org.springframework.beans.factory.annotation.Qualifier("legacyEntityManagerFactory")
        emf: org.springframework.orm.jpa.EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)

    @Bean(name = ["newTransactionManager"])
    fun newTransactionManager(
        @org.springframework.beans.factory.annotation.Qualifier("newEntityManagerFactory")
        emf: org.springframework.orm.jpa.EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)
}

package com.example.dtx.twopc.config

import com.atomikos.icatch.jta.UserTransactionImp
import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.jdbc.AtomikosDataSourceBean
import com.example.dtx.twopc.domain.LegacyMember
import com.example.dtx.twopc.domain.NewMember
import jakarta.transaction.TransactionManager
import jakarta.transaction.UserTransaction
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.jta.JtaTransactionManager
import java.util.Properties

/**
 * 2PC 코디네이터 설정 — Atomikos(JTA).
 *
 * - 두 개의 XA DataSource(legacy H2 / new H2)를 Atomikos 가 관리
 * - 각 EntityManagerFactory 는 jta=true 로 JTA 분산 트랜잭션에 참여
 * - @Transactional 하나로 양쪽 DB 쓰기가 원자적으로 묶임
 *   → 이게 곧 2PC (Phase 1 prepare / Phase 2 commit 은 Atomikos 가 수행)
 */
@Configuration
@EnableTransactionManagement
class JtaConfig {

    private fun h2Xa(url: String, resourceName: String) = AtomikosDataSourceBean().apply {
        uniqueResourceName = resourceName
        xaDataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
        poolSize = 5
        xaProperties = Properties().apply {
            setProperty("URL", url)
            setProperty("user", "sa")
            setProperty("password", "")
        }
    }

    @Bean(name = ["legacyDataSource"])
    fun legacyDataSource() = h2Xa("jdbc:h2:mem:legacy;DB_CLOSE_DELAY=-1;MODE=MySQL", "legacy")

    @Bean(name = ["newDataSource"])
    fun newDataSource() = h2Xa("jdbc:h2:mem:newdb;DB_CLOSE_DELAY=-1;MODE=MySQL", "newdb")

    @Primary
    @Bean(name = ["legacyEntityManagerFactory"])
    fun legacyEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(legacyDataSource())
            .packages(LegacyMember::class.java)
            .persistenceUnit("legacy")
            .jta(true)
            .build()

    @Bean(name = ["newEntityManagerFactory"])
    fun newEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(newDataSource())
            .packages(NewMember::class.java)
            .persistenceUnit("new")
            .jta(true)
            .build()

    @Bean
    fun atomikosUserTransaction(): UserTransaction = UserTransactionImp().apply { transactionTimeout = 300 }

    @Bean(initMethod = "init", destroyMethod = "close")
    fun atomikosTransactionManager(): UserTransactionManager = UserTransactionManager()

    @Bean
    fun transactionManager(
        userTransaction: UserTransaction,
        tm: TransactionManager,
    ): PlatformTransactionManager = JtaTransactionManager(userTransaction, tm)
}

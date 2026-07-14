package com.example.dtx.saga.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import java.util.Properties
import javax.sql.DataSource

/**
 * Saga 는 JTA(2PC) 없이 각 DB 마다 독립 로컬 트랜잭션 매니저를 둔다.
 * Orchestrator(MemberMigrationSaga) 가 단계별로 호출하고, 실패 시 보상 트랜잭션을 직접 호출한다.
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

    private fun emf(ds: DataSource, unit: String, pkg: String): LocalContainerEntityManagerFactoryBean =
        LocalContainerEntityManagerFactoryBean().apply {
            dataSource = ds
            setPackagesToScan(pkg)
            persistenceUnitName = unit
            jpaVendorAdapter = HibernateJpaVendorAdapter()
            setJpaProperties(Properties().apply { setProperty("hibernate.hbm2ddl.auto", "update") })
        }

    @Bean(name = ["legacyEntityManagerFactory"])
    @Primary
    fun legacyEntityManagerFactory(): LocalContainerEntityManagerFactoryBean =
        emf(legacyDataSource(), "legacy", "com.example.dtx.saga.domain.legacy")

    @Bean(name = ["newEntityManagerFactory"])
    fun newEntityManagerFactory(): LocalContainerEntityManagerFactoryBean =
        emf(newDataSource(), "new", "com.example.dtx.saga.domain.newdb")

    @Bean(name = ["legacyTransactionManager"])
    @Primary
    fun legacyTransactionManager(
        @Qualifier("legacyEntityManagerFactory") emf: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)

    @Bean(name = ["newTransactionManager"])
    fun newTransactionManager(
        @Qualifier("newEntityManagerFactory") emf: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)
}

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.saga.repository.legacy"],
    entityManagerFactoryRef = "legacyEntityManagerFactory",
    transactionManagerRef = "legacyTransactionManager",
)
class LegacyJpaConfig

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.saga.repository.newdb"],
    entityManagerFactoryRef = "newEntityManagerFactory",
    transactionManagerRef = "newTransactionManager",
)
class NewJpaConfig

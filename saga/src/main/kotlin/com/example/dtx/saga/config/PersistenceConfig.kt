package com.example.dtx.saga.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
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
 * Saga 는 JTA(2PC) 없이 각 DB 마다 독립 로컬 트랜잭션 매니저.
 * profile / contact 두 신규 DB 각각 자체 TM. Orchestrator 가 단계별로 호출 + 보상.
 */
@Configuration
class PersistenceConfig {

    private fun h2(url: String): DataSource = DriverManagerDataSource().apply {
        setDriverClassName("org.h2.Driver")
        setUrl(url)
        username = "sa"
        password = ""
    }

    @Bean(name = ["profileDataSource"])
    @Primary
    fun profileDataSource(): DataSource = h2("jdbc:h2:mem:profile;DB_CLOSE_DELAY=-1;MODE=MySQL")

    @Bean(name = ["contactDataSource"])
    fun contactDataSource(): DataSource = h2("jdbc:h2:mem:contact;DB_CLOSE_DELAY=-1;MODE=MySQL")

    @Bean
    @Primary
    fun entityManagerFactoryBuilder(): EntityManagerFactoryBuilder =
        EntityManagerFactoryBuilder(HibernateJpaVendorAdapter(), { mutableMapOf<String, Any>() }, null)

    @Bean(name = ["profileEntityManagerFactory"])
    @Primary
    fun profileEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(profileDataSource())
            .packages("com.example.dtx.saga.domain.profile")
            .persistenceUnit("profile")
            .properties(mapOf("hibernate.hbm2ddl.auto" to "update"))
            .build()

    @Bean(name = ["contactEntityManagerFactory"])
    fun contactEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(contactDataSource())
            .packages("com.example.dtx.saga.domain.contact")
            .persistenceUnit("contact")
            .properties(mapOf("hibernate.hbm2ddl.auto" to "update"))
            .build()

    @Bean(name = ["profileTransactionManager"])
    @Primary
    fun profileTransactionManager(
        @Qualifier("profileEntityManagerFactory") emf: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)

    @Bean(name = ["contactTransactionManager"])
    fun contactTransactionManager(
        @Qualifier("contactEntityManagerFactory") emf: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(emf)
}

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.saga.repository.profile"],
    entityManagerFactoryRef = "profileEntityManagerFactory",
    transactionManagerRef = "profileTransactionManager",
)
class ProfileJpaConfig

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.saga.repository.contact"],
    entityManagerFactoryRef = "contactEntityManagerFactory",
    transactionManagerRef = "contactTransactionManager",
)
class ContactJpaConfig

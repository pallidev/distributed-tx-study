package com.example.dtx.twopc.config

import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.jdbc.AtomikosDataSourceBean
import jakarta.transaction.TransactionManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.jta.JtaTransactionManager
import java.util.Properties
import javax.sql.DataSource

/**
 * 2PC 코디네이터(Atomikos/JTA) + 신규 DB 2개(profile/contact).
 *
 * 시나리오: 레거시 단일 회원 테이블(컬럼 다수)을 도메인별로 분해 →
 *  - 신규 DB A (profile): 회원 기본정보
 *  - 신규 DB B (contact): 회원 연락처/계정
 * 두 DB 를 한 JTA 트랜잭션(2PC)으로 묶어 한 회원의 분할된 데이터를 원자적으로 쓴다.
 *
 * 핵심: EntityManagerFactoryBuilder.jta(true) 로 Hibernate 가 JTA 글로벌 TX 에 참여해야
 *  persist→flush→XA branch enlist→commit 흐름이 일어난다.
 */
@Configuration
@EnableTransactionManagement
class JtaConfig(
    @Value("\${datasource.profile.xa-class}") private val profileXaClass: String,
    @Value("\${datasource.profile.url}") private val profileUrl: String,
    @Value("\${datasource.contact.xa-class}") private val contactXaClass: String,
    @Value("\${datasource.contact.url}") private val contactUrl: String,
    @Value("\${datasource.user:sa}") private val user: String,
    @Value("\${datasource.password:}") private val password: String,
    @Value("\${hibernate.hbm2ddl.auto:update}") private val hbm2ddl: String,
) {
    private fun xa(url: String, name: String, xaClass: String): DataSource = AtomikosDataSourceBean().apply {
        uniqueResourceName = name
        xaDataSourceClassName = xaClass
        maxPoolSize = 5
        xaProperties = Properties().apply {
            setProperty("URL", url)
            setProperty("user", user)
            setProperty("password", password)
            // MySQL Connector/J XA: 같은 XID(글로벌 TX)는 항상 같은 physical connection 으로 라우팅
            setProperty("pinGlobalTxToPhysicalConnection", "true")
        }
    }

    @Bean(name = ["profileDataSource"])
    fun profileDataSource(): DataSource = xa(profileUrl, "profile", profileXaClass)

    @Bean(name = ["contactDataSource"])
    fun contactDataSource(): DataSource = xa(contactUrl, "contact", contactXaClass)

    @Bean
    @Primary
    fun entityManagerFactoryBuilder(): EntityManagerFactoryBuilder =
        EntityManagerFactoryBuilder(HibernateJpaVendorAdapter(), { mutableMapOf<String, Any>() }, null)

    @Primary
    @Bean(name = ["profileEntityManagerFactory"])
    fun profileEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(profileDataSource())
            .packages("com.example.dtx.twopc.domain.profile")
            .persistenceUnit("profile")
            .jta(true)
            .properties(mapOf("hibernate.hbm2ddl.auto" to hbm2ddl))
            .build()

    @Bean(name = ["contactEntityManagerFactory"])
    fun contactEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(contactDataSource())
            .packages("com.example.dtx.twopc.domain.contact")
            .persistenceUnit("contact")
            .jta(true)
            .properties(mapOf("hibernate.hbm2ddl.auto" to hbm2ddl))
            .build()

    @Bean(initMethod = "init", destroyMethod = "close")
    fun atomikosTransactionManager(): UserTransactionManager = UserTransactionManager().apply { setTransactionTimeout(300) }

    @Bean
    fun transactionManager(utm: UserTransactionManager): PlatformTransactionManager = JtaTransactionManager(utm, utm)
}

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.profile"],
    entityManagerFactoryRef = "profileEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class ProfileJpaConfig

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.contact"],
    entityManagerFactoryRef = "contactEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class ContactJpaConfig

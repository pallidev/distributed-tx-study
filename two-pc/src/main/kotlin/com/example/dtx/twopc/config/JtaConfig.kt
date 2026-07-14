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
 * 2PC 코디네이터(Atomikos/JTA) + 다중 DataSource/EMF.
 *
 * 핵심: EntityManagerFactoryBuilder.jta(true) 로 Hibernate 가 JTA 글로벌 TX 에 참여해야
 *  persist → flush → XA branch enlist → commit 흐름이 일어난다.
 *  (LocalContainerEntityManagerFactoryBean 직접 생성 시 jta(true) 가 빠지면 insert 자체가 안 된다.)
 */
@Configuration
@EnableTransactionManagement
class JtaConfig(
    @Value("\${datasource.legacy.xa-class}") private val legacyXaClass: String,
    @Value("\${datasource.legacy.url}") private val legacyUrl: String,
    @Value("\${datasource.new.xa-class}") private val newXaClass: String,
    @Value("\${datasource.new.url}") private val newUrl: String,
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

    @Bean(name = ["legacyDataSource"])
    fun legacyDataSource(): DataSource = xa(legacyUrl, "legacy", legacyXaClass)

    @Bean(name = ["newDataSource"])
    fun newDataSource(): DataSource = xa(newUrl, "newdb", newXaClass)

    // 다중 DataSource 라 Boot JPA auto-config 가 EntityManagerFactoryBuilder 빈을 안 만들어 주므로 직접 생성.
    @Bean
    @Primary
    fun entityManagerFactoryBuilder(): EntityManagerFactoryBuilder =
        EntityManagerFactoryBuilder(HibernateJpaVendorAdapter(), { mutableMapOf<String, Any>() }, null)

    @Primary
    @Bean(name = ["legacyEntityManagerFactory"])
    fun legacyEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(legacyDataSource())
            .packages("com.example.dtx.twopc.domain.legacy")
            .persistenceUnit("legacy")
            .jta(true) // ★ Hibernate 가 JTA 글로벌 TX 에 참여
            .properties(mapOf("hibernate.hbm2ddl.auto" to hbm2ddl))
            .build()

    @Bean(name = ["newEntityManagerFactory"])
    fun newEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(newDataSource())
            .packages("com.example.dtx.twopc.domain.newdb")
            .persistenceUnit("new")
            .jta(true) // ★
            .properties(mapOf("hibernate.hbm2ddl.auto" to hbm2ddl))
            .build()

    @Bean(initMethod = "init", destroyMethod = "close")
    fun atomikosTransactionManager(): UserTransactionManager = UserTransactionManager().apply { setTransactionTimeout(300) }

    @Bean
    fun transactionManager(utm: UserTransactionManager): PlatformTransactionManager = JtaTransactionManager(utm, utm)
}

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.legacy"],
    entityManagerFactoryRef = "legacyEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class LegacyJpaConfig

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.newdb"],
    entityManagerFactoryRef = "newEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class NewJpaConfig

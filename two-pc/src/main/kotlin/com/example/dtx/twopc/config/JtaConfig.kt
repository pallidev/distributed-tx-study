package com.example.dtx.twopc.config

import com.atomikos.icatch.jta.UserTransactionImp
import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.jdbc.AtomikosDataSourceBean
import jakarta.transaction.TransactionManager
import jakarta.transaction.UserTransaction
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
 * 2PC 코디네이터(Atomikos/JTA) + 다중 DataSource/EMF 설정.
 *  - legacy/new 두 XA DataSource 를 Atomikos 가 관리
 *  - 각 EMF 는 JTA 에 참여 (Hibernate JTA platform = Atomikos)
 *  - @Transactional 하나로 양쪽 DB 쓰기가 원자적으로 묶임 = 2PC
 */
@Configuration
@EnableTransactionManagement
class JtaConfig {

    private fun h2Xa(url: String, name: String): DataSource = AtomikosDataSourceBean().apply {
        uniqueResourceName = name
        xaDataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
        maxPoolSize = 5
        xaProperties = Properties().apply {
            setProperty("URL", url)
            setProperty("user", "sa")
            setProperty("password", "")
        }
    }

    @Bean(name = ["legacyDataSource"])
    fun legacyDataSource(): DataSource = h2Xa("jdbc:h2:mem:legacy;DB_CLOSE_DELAY=-1;MODE=MySQL", "legacy")

    @Bean(name = ["newDataSource"])
    fun newDataSource(): DataSource = h2Xa("jdbc:h2:mem:newdb;DB_CLOSE_DELAY=-1;MODE=MySQL", "newdb")

    private fun emf(ds: DataSource, unit: String, pkg: String): LocalContainerEntityManagerFactoryBean =
        LocalContainerEntityManagerFactoryBean().apply {
            dataSource = ds
            setPackagesToScan(pkg)
            persistenceUnitName = unit
            jpaVendorAdapter = HibernateJpaVendorAdapter()
            setJpaProperties(
                Properties().apply {
                    setProperty("hibernate.transaction.jta.platform", "Atomikos")
                    setProperty("hibernate.hbm2ddl.auto", "update")
                },
            )
        }

    @Primary
    @Bean(name = ["legacyEntityManagerFactory"])
    fun legacyEntityManagerFactory(): LocalContainerEntityManagerFactoryBean =
        emf(legacyDataSource(), "legacy", "com.example.dtx.twopc.domain.legacy")

    @Bean(name = ["newEntityManagerFactory"])
    fun newEntityManagerFactory(): LocalContainerEntityManagerFactoryBean =
        emf(newDataSource(), "new", "com.example.dtx.twopc.domain.newdb")

    @Bean(initMethod = "init", destroyMethod = "close")
    fun atomikosTransactionManager(): UserTransactionManager = UserTransactionManager().apply { setTransactionTimeout(300) }

    @Bean
    fun transactionManager(utm: UserTransactionManager): PlatformTransactionManager =
        JtaTransactionManager(utm, utm)
}

/** legacy DB용 JPA Repository → legacyEntityManagerFactory + JTA transactionManager */
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.legacy"],
    entityManagerFactoryRef = "legacyEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class LegacyJpaConfig

/** new DB용 JPA Repository → newEntityManagerFactory + JTA transactionManager */
@Configuration
@EnableJpaRepositories(
    basePackages = ["com.example.dtx.twopc.repository.newdb"],
    entityManagerFactoryRef = "newEntityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class NewJpaConfig

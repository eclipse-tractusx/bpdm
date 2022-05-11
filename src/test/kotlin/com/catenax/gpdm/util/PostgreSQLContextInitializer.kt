package com.catenax.gpdm.util

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

/**
 * When used on a spring boot test, starts a singleton postgres db container that is shared between all integration tests.
 */
class PostgreSQLContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        val postgreSQLContainer = PostgreSQLContainer("postgres:13.2")
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgreSQLContainer.start()
        TestPropertyValues.of(
            "spring.datasource.url=${postgreSQLContainer.jdbcUrl}",
            "spring.datasource.username=${postgreSQLContainer.username}",
            "spring.datasource.password=${postgreSQLContainer.password}"
        ).applyTo(applicationContext.environment)
    }
}
package com.catenax.gpdm.util

import org.testcontainers.containers.PostgreSQLContainer

object PostgreSQLSingletonContainer {

    val instance by lazy { start() }

    private fun start() = PostgreSQLContainer("postgres:13.2")
        .apply {
            start()
        }
}
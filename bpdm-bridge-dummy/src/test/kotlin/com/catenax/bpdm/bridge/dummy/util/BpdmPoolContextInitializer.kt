/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package com.catenax.bpdm.bridge.dummy.util


import com.catenax.bpdm.bridge.dummy.util.PostgreSQLContextInitializer.Companion.postgreSQLContainer
import mu.KotlinLogging
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.lifecycle.Startable
import java.time.Duration

/**
 * When used on a spring boot test, starts a singleton postgres db container that is shared between all integration tests.
 */


class BpdmPoolContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    private val logger = KotlinLogging.logger { }

    companion object {
        const val POOL_CONTAINER_STARTUP_TIMEOUT_SEC = 300L
        const val BPDM_PORT = 8080
        const val DEBUG_PORT = 8050
        const val IMAGE = "maven-pool"

        val bpdmPoolContainer: GenericContainer<*> =
            GenericContainer(IMAGE)
                .dependsOn(listOf<Startable>(postgreSQLContainer))
                .withNetwork(postgreSQLContainer.network)
                .withExposedPorts(BPDM_PORT, DEBUG_PORT)
                .withEnv(
                    "JAVA_OPTIONS",
                    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:$DEBUG_PORT"
                )
    }


    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val postgresNetworkAlias = applicationContext.environment.getProperty("bpdm.datasource.alias")
        val dataBase = postgreSQLContainer.getDatabaseName()
        val bpdmAlias = applicationContext.environment.getProperty("bpdm.pool.alias")
        bpdmPoolContainer.withNetworkAliases(bpdmAlias)
            .waitingFor(
                HttpWaitStrategy()
                    .forPort(BPDM_PORT)
                    .forStatusCodeMatching { response -> response == 200 || response == 401 }
                    .withStartupTimeout(Duration.ofSeconds(POOL_CONTAINER_STARTUP_TIMEOUT_SEC))
            )

        bpdmPoolContainer.withEnv(
            "spring.datasource.url", "jdbc:postgresql://${postgresNetworkAlias}:5432/${dataBase}?loggerLevel=OFF"
        )
            .withEnv(
                "spring.datasource.username", postgreSQLContainer.username
            )
            .withEnv(
                "spring.datasource.password", postgreSQLContainer.password
            )
            .start()

        TestPropertyValues.of(
            "bpdm.pool.base-url=http://localhost:${bpdmPoolContainer.getMappedPort(BPDM_PORT)}",
        ).applyTo(applicationContext.environment)

        logger.info { "[!!!] Pool can be remote-debugged on port ${bpdmPoolContainer.getMappedPort(DEBUG_PORT)} " }
    }
}
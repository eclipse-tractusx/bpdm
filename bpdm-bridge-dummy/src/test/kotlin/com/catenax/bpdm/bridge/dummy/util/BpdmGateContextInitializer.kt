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


import com.catenax.bpdm.bridge.dummy.util.BpdmPoolContextInitializer.Companion.bpdmPoolContainer
import com.catenax.bpdm.bridge.dummy.util.OpenSearchContextInitializer.Companion.openSearchContainer
import com.catenax.bpdm.bridge.dummy.util.PostgreSQLContextInitializer.Companion.postgreSQLContainer
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


class BpdmGateContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        const val GATE_CONTAINER_STARTUP_TIMEOUT_SEC = 180L
        const val BPDM_PORT = 8081
        const val IMAGE = "maven-gate"

        private val bpdmGateContainer: GenericContainer<*> =
            GenericContainer(IMAGE)
                .dependsOn(listOf<Startable>(postgreSQLContainer, openSearchContainer, bpdmPoolContainer))
                .withNetwork(postgreSQLContainer.getNetwork())
                .withExposedPorts(BPDM_PORT)


    }


    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val postgresNetworkAlias = applicationContext.environment.getProperty("bpdm.datasource.alias")
        val bpdmPoolAlias = applicationContext.environment.getProperty("bpdm.pool.alias")
        val dataBase = postgreSQLContainer.getDatabaseName()
        bpdmGateContainer.waitingFor(
            HttpWaitStrategy()
                .forPort(BPDM_PORT)
                .forStatusCodeMatching { response -> response == 200 || response == 401 }
                .withStartupTimeout(Duration.ofSeconds(GATE_CONTAINER_STARTUP_TIMEOUT_SEC))
        )
        bpdmGateContainer.withEnv(
            "spring.datasource.url", "jdbc:postgresql://${postgresNetworkAlias}:5432/${dataBase}?loggerLevel=OFF"
        )
            .withEnv("bpdm.pool.base-url", "http://$bpdmPoolAlias:8080/api/catena")

            .withEnv(
                "spring.datasource.username", postgreSQLContainer.username
            )
            .withEnv(
                "spring.datasource.password", postgreSQLContainer.password
            ).start()


        TestPropertyValues.of(
            "bpdm.gate.base-url=http://localhost:${bpdmGateContainer.getMappedPort(BPDM_PORT)}",
        ).applyTo(applicationContext.environment)

    }
}
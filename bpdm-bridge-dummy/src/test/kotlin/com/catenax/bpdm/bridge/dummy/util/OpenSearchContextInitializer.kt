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


import com.github.dockerjava.api.model.Ulimit
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

/**
 * When used on a spring boot test, starts a singleton opensearch container that is shared between all integration tests.
 */

class OpenSearchContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    companion object {
        const val OPENSEARCH_PORT = 9200
        val openSearchContainer: GenericContainer<*> = GenericContainer("opensearchproject/opensearch:2.1.0")
            .withExposedPorts(OPENSEARCH_PORT)
            .waitingFor(HttpWaitStrategy()
                .forPort(OPENSEARCH_PORT)
                .forStatusCodeMatching { response -> response == 200 || response == 401 }
            )
            // based on sample docker-compose for development from https://opensearch.org/docs/latest/opensearch/install/docker
            .withEnv("cluster.name", "cdqbridge")
            .withEnv("node.name", "bpdm-opensearch")
            .withEnv("bootstrap.memory_lock", "true")
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
            .withEnv("DISABLE_SECURITY_PLUGIN", "true")
            .withEnv("discovery.type", "single-node")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!.withUlimits(arrayOf(Ulimit("nofile", 65536L, 65536L), Ulimit("memlock", -1L, -1L)))
            }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val openSearchAlias = applicationContext.environment.getProperty("bpdm.opensearch.alias")
        openSearchContainer.withNetworkAliases(openSearchAlias)
        openSearchContainer.start()
        TestPropertyValues.of(
            "bpdm.opensearch.host=${openSearchContainer.host}",
            "bpdm.opensearch.port=${openSearchContainer.getMappedPort(OPENSEARCH_PORT)}",
            "bpdm.opensearch.scheme=http",
            "bpdm.opensearch.enabled=true",
        ).applyTo(applicationContext.environment)
    }
}
/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.v7

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.PoolTestBase
import org.eclipse.tractusx.bpdm.pool.UnscheduledTestEnvironment
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.web.reactive.function.client.WebClient

@UnscheduledTestEnvironment
class AccessGroupOpenApiIT @Autowired constructor(
    webServerAppCtxt: ServletWebServerApplicationContext
) : PoolTestBase() {

    private val webClient = WebClient.create("http://localhost:${webServerAppCtxt.webServer!!.port}")
    private val objectMapper = ObjectMapper()

    @Test
    fun `every access group is a non-empty subset of the full v7 API`() {
        val allOperations = operationsOf("v7")

        ACCESS_GROUPS.forEach { group ->
            val groupOperations = operationsOf(group)

            assertThat(groupOperations)
                .describedAs("operations of group '%s'", group)
                .isNotEmpty()
            assertThat(allOperations)
                .describedAs("operations of group '%s' that the full v7 API does not contain", group)
                .containsAll(groupOperations)
        }
    }

    @Test
    fun `every v7 endpoint is granted by at least one access group`() {
        val allOperations = operationsOf("v7")
        val grantedOperations = ACCESS_GROUPS.flatMap { operationsOf(it) }.toSet()

        assertThat(allOperations.minus(grantedOperations))
            .describedAs("v7 endpoints reachable through no access group - they declare a permission that belongs to no user group, or no permission at all")
            .isEmpty()
    }

    @Test
    fun `an admin is granted endpoints a dataspace participant is not`() {
        val participantOperations = operationsOf("v7-participant")
        val adminOperations = operationsOf("v7-admin")

        assertThat(adminOperations.minus(participantOperations))
            .describedAs("endpoints granted to an admin but not to a dataspace participant")
            .isNotEmpty()
    }

    private fun operationsOf(group: String): Set<String> {
        val document = webClient.get().uri("/docs/api-docs/$group").retrieve().bodyToMono(String::class.java).block()
        val paths = objectMapper.readTree(document).get("paths") ?: return emptySet()

        return paths.properties().flatMap { (path, methods) ->
            methods.properties().map { (method, _) -> "${method.uppercase()} $path" }
        }.toSet()
    }

    companion object {
        private val ACCESS_GROUPS = listOf("v7-participant", "v7-sharing-member", "v7-admin")
    }
}

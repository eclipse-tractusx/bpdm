/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.orchestrator.controller

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.orchestrator.repository.OriginRegistrarRepository
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.PriorityEnum
import org.eclipse.tractusx.orchestrator.api.model.UpsertOriginRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "bpdm.security.enabled=false"
    ]
)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class OriginRegistrarControllerIT  @Autowired constructor(
    private val orchestratorClient: OrchestrationApiClient,
    private val originRegistrarRepository: OriginRegistrarRepository,
) {

    @BeforeEach
    fun cleanUp() {
        originRegistrarRepository.deleteAll()
    }

    @Test
    fun `register new gate with threshold`(){
        registerAndValidatingGateInformation()
    }

    @Test
    fun `fetch registered gate information`(){
        val originId = registerAndValidatingGateInformation()
        var response = orchestratorClient.originRegistrar.fetchOrigin(originId)
        assertThat(response).isNotNull()
        assertThat(response.originId).isNotNull().isNotBlank()
        Assertions.assertEquals(originId, response.originId)
    }

    @Test
    fun `update registered gate information`(){
        val originId = registerAndValidatingGateInformation()
        val request = UpsertOriginRequest(5, "Update test", PriorityEnum.Low)
        val response = orchestratorClient.originRegistrar.updateOrigin(originId, request)
        assertThat(response).isNotNull()
        assertThat(response.originId).isNotNull().isNotBlank()
        Assertions.assertEquals(request.priority, response.priority)
        Assertions.assertEquals(request.name, response.name)
        Assertions.assertEquals(1L, originRegistrarRepository.count())
    }

    private fun registerAndValidatingGateInformation(): String {
        Assertions.assertEquals(0L, originRegistrarRepository.count())
        val request = UpsertOriginRequest(20, "Test", PriorityEnum.High)
        val response = orchestratorClient.originRegistrar.registerOrigin(request)
        assertThat(response).isNotNull()
        assertThat(response.originId).isNotNull().isNotBlank()
        Assertions.assertEquals(request.priority, response.priority)
        Assertions.assertEquals(request.name, response.name)
        Assertions.assertEquals(1L, originRegistrarRepository.count())
        assertThat(originRegistrarRepository.findByOriginId(response.originId)).isNotNull
        return response.originId
    }
}
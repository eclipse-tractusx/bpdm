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

package org.eclipse.tractusx.bpdm.pool.util

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.config.OrchestratorClientConfigProperties
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.TaskStateRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

@Component("orchestratorHealth")
class OrchestratorHealthIndicator(
    private val orchestratorConfigProperties: OrchestratorClientConfigProperties,
    @Qualifier("orchestratorWebClient") private val webClient: WebClient
) : HealthIndicator {

    override fun health(): Health {

        val orchestratorHealthUrl = "${orchestratorConfigProperties.baseUrl}/actuator/health"

        return try {
            val response = webClient.get()
                .uri(orchestratorHealthUrl)
                .retrieve()
                .toEntity(String::class.java)
                .block()

            if (response?.statusCode?.is2xxSuccessful == true) {
                Health.up().withDetail("Orchestrator Service", "Available").build()
            } else {
                Health.down().withDetail("Orchestrator Service", "Unreachable").build()
            }
        } catch (e: Exception) {
            Health.down().withDetail("Orchestrator Service", "Error: ${e.message}").build()
        }
    }
}
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

package org.eclipse.tractusx.bpdm.gate.config

import io.mockk.every
import io.mockk.mockk
import org.eclipse.tractusx.bpdm.gate.util.OrchestratorHealthIndicator
import org.eclipse.tractusx.bpdm.gate.util.PoolHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MockHealthIndicatorConfig {

    @Bean
    fun poolHealthIndicator(): PoolHealthIndicator {
        return mockk<PoolHealthIndicator> {
            every { health() } returns Health.up().build()
        }
    }

    @Bean
    fun orchestratorHealthIndicator(): OrchestratorHealthIndicator {
        return mockk<OrchestratorHealthIndicator> {
            every { health() } returns Health.up().build()
        }
    }
}
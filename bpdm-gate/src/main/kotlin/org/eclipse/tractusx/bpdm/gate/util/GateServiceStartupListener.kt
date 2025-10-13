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

package org.eclipse.tractusx.bpdm.gate.util

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.service.DependencyHealthService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class GateServiceStartupListener(
    private val dependencyHealthService: DependencyHealthService
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = KotlinLogging.logger { }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val healthStatus = dependencyHealthService.checkAllDependencies()
        val unhealthyDependencies = healthStatus.filter { it.value == "Down" }

        if (unhealthyDependencies.isNotEmpty()) {
            logger.error("Startup failed. Dependencies not ready: ${unhealthyDependencies.map { "${it.key}: ${it.value}" }.joinToString(", ")}")
            throw IllegalStateException("Dependencies not ready: ${unhealthyDependencies.map { "${it.key}: ${it.value}" }.joinToString(", ")}")
        } else {
            logger.info("All dependencies are healthy on startup: ${healthStatus.map { "${it.key}: ${it.value}" }.joinToString(", ")}")
        }
    }

}

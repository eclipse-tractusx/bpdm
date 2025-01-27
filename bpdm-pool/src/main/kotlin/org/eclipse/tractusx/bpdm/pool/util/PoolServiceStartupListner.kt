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

package org.eclipse.tractusx.bpdm.pool.util

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.service.DependencyHealthService
import org.eclipse.tractusx.bpdm.pool.service.TaskBatchResolutionService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class PoolServiceStartupListner(
    private val dependencyHealthService: DependencyHealthService,
    private val taskBatchResolutionService: TaskBatchResolutionService
) : ApplicationListener<ApplicationStartedEvent> {

    private val logger = KotlinLogging.logger { }

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        try{
            taskBatchResolutionService.processTasks()
        }catch (ex: Throwable){
            throw IllegalStateException("Could not resolve tasks: ${ex.message}")
        }
    }

}
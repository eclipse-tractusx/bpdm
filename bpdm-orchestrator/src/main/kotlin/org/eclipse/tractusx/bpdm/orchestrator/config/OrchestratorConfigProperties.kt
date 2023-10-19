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

package org.eclipse.tractusx.bpdm.orchestrator.config

import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "bpdm.orchestrator-security")
data class OrchestratorConfigProperties(
    private val createTask: String = "create_task",
    private val viewTask: String = "view_task",
    private val processTaskPrefix: String = "process_task_step"
) {
    fun roleCreateTask() =
        "ROLE_$createTask"

    fun roleViewTask() =
        "ROLE_$viewTask"

    fun roleProcessTask(step: TaskStep) =
        "ROLE_${processTaskPrefix}_${step.name}"
}

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

import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties.Companion.PREFIX
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = PREFIX)
data class PermissionConfigProperties(
    val createTask: String = "create_task",
    val viewTask: String = "view_task",
    val processTask: ProcessTaskProperties = ProcessTaskProperties()
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"

        //Keep the fully qualified name up to data here
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val CREATE_TASK = "@$BEAN_QUALIFIER.getCreateTask()"
        const val VIEW_TASK = "@$BEAN_QUALIFIER.getViewTask()"
        const val GET_PROCESS_TASK = "@$BEAN_QUALIFIER.getProcessTask"
    }

    @Suppress("unused")
    fun getProcessTask(step: TaskStep): String {
        return when (step) {
            TaskStep.CleanAndSync -> processTask.cleanAndSync
            TaskStep.PoolSync -> processTask.poolSync
            TaskStep.Clean -> processTask.clean
        }
    }

    data class ProcessTaskProperties(
        val clean: String = toDefaultValue(TaskStep.Clean),
        val cleanAndSync: String = toDefaultValue(TaskStep.CleanAndSync),
        val poolSync: String = toDefaultValue(TaskStep.PoolSync)
    ) {
        companion object {
            fun toDefaultValue(step: TaskStep) = "process_task_step_${step.name}"
        }
    }
}



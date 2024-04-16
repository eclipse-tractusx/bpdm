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

package org.eclipse.tractusx.bpdm.orchestrator.config

import org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties.Companion.PREFIX
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = PREFIX)
data class PermissionConfigProperties(
    val createTask: String = "create_task",
    val readTask: String = "read_task",
    val reservations: TaskStepProperties = TaskStepProperties(
        clean = "create_reservation_clean",
        cleanAndSync = "create_reservation_cleanAndSync",
        poolSync = "create_reservation_poolSync"
    ),
    val results: TaskStepProperties = TaskStepProperties(
        clean = "create_result_clean",
        cleanAndSync = "create_result_cleanAndSync",
        poolSync = "create_result_poolSync"
    )
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"

        //Keep the fully qualified name up to data here
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val CREATE_TASK = "@$BEAN_QUALIFIER.getCreateTask()"
        const val VIEW_TASK = "@$BEAN_QUALIFIER.getReadTask()"
        const val INVOKE_CREATE_RESERVATION = "@$BEAN_QUALIFIER.createReservation"
        const val INVOKE_CREATE_RESULT = "@$BEAN_QUALIFIER.createResult"
    }

    @Suppress("unused")
    fun createReservation(step: TaskStep): String{
        return fetchStepPermission(step, reservations)
    }

    @Suppress("unused")
    fun createResult(step: TaskStep): String{
        return fetchStepPermission(step, results)
    }


    private fun fetchStepPermission(step: TaskStep, stepProperties: TaskStepProperties): String{
        return when (step) {
            TaskStep.CleanAndSync -> stepProperties.cleanAndSync
            TaskStep.PoolSync -> stepProperties.poolSync
            TaskStep.Clean -> stepProperties.clean
        }
    }

    data class TaskStepProperties(
        val clean: String,
        val cleanAndSync: String,
        val poolSync: String
    )
}



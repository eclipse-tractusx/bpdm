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
    val reservation:  Map<TaskStep, String> = TaskStep.entries.associateWith { "create_reservation_${it.name}" },
    val result:  Map<TaskStep, String> = TaskStep.entries.associateWith { "create_result_${it.name}" }
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"

        //Keep the fully qualified name up to date here
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.orchestrator.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val CREATE_TASK = "@$BEAN_QUALIFIER.getCreateTask()"
        const val VIEW_TASK = "@$BEAN_QUALIFIER.getReadTask()"
    }
}



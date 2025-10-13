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

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "bpdm.tasks")
data class GoldenRecordTaskConfigProperties(
    val creation: CreationProperties = CreationProperties(),
    val check: TaskProcessProperties = TaskProcessProperties(),
    val healthCheck: TaskProcessProperties = TaskProcessProperties(),
    val dependencyCheck: TaskProcessProperties = TaskProcessProperties(),
    val consistencyCheck: TaskProcessProperties = TaskProcessProperties(),
    val relationCreation: TaskProcessProperties = TaskProcessProperties(),
    val relationCheck: TaskProcessProperties = TaskProcessProperties()
) {
    data class CreationProperties(
        val fromSharingMember: CreationTaskProperties = CreationTaskProperties(),
        val fromPool: TaskProcessProperties = TaskProcessProperties()
    )

    data class TaskProcessProperties(
        val batchSize: Int = 20,
        val cron: String = "-",
    )

    data class CreationTaskProperties(
        val startsAsReady: Boolean = true,
        val batchSize: Int = 20,
        val cron: String = "-",
    )
}
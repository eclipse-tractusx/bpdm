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

package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "bpdm.tasks")
data class GoldenRecordTaskConfigProperties(
    val creation: CreationProperties = CreationProperties(),
    val check: TaskProcessProperties = TaskProcessProperties()
) {
    data class CreationProperties(
        val fromSharingMember: TaskProcessProperties = TaskProcessProperties(),
        val fromPool: TaskProcessProperties = TaskProcessProperties()
    )

    data class TaskProcessProperties(
        var batchSize: Int = 100,
        var cron: String = "-",
    )
}
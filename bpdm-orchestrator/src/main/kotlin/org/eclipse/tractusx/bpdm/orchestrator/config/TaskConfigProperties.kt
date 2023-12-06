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

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "bpdm.task")
data class TaskConfigProperties(
    // Cron expression for checking if any of the timeouts has been reached
    val timeoutCheckCron: String = "-",
    // Timeout after which a pending task should change into state "Error" with error type "Timeout" starting with its creation.
    val taskPendingTimeout: Duration = Duration.ofDays(2),
    // Timeout after which a task should be removed from the Orchestrator starting with its creation.
    val taskRetentionTimeout: Duration = Duration.ofDays(14),
)

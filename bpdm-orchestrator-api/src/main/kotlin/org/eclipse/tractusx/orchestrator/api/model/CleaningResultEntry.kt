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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "A cleaning result for a cleaning task")
data class CleaningResultEntry(
    @get:Schema(description = "The identifier of the cleaning task for which this is a result", required = true)
    val taskId: String,
    @get:Schema(description = "The actual result in form of business partner data. Maybe null if an error occurred during cleaning of this task.")
    val result: BusinessPartnerFull? = null,
    @get:ArraySchema(arraySchema = Schema(description = "Errors that occurred during cleaning of this task"))
    val errors: List<TaskError> = emptyList()
)

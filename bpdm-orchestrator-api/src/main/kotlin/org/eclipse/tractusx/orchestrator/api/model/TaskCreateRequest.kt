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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object to specify for which business partner data tasks should be created and in which mode")
data class TaskCreateRequest(

    @get:Schema(required = true, description = "The mode affecting which processing steps the business partner goes through")
    val mode: TaskMode,

    @get:ArraySchema(arraySchema = Schema(description = "The list of tasks to create"))
    val requests: List<TaskCreateRequestEntry>,

    @get:Schema(required = true, description = "Indicates the originator of the record")
    val originId: String,
)

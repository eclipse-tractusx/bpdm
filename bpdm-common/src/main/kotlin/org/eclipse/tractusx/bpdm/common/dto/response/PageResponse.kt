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

package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Paginated collection of results")
data class PageResponse<T>(
    @get:Schema(description = "Total number of all results in all pages")
    val totalElements: Long,
    @get:Schema(description = "Total number pages")
    val totalPages: Int,
    @get:Schema(description = "Current page number")
    val page: Int,
    @get:Schema(description = "Number of results in the page")
    val contentSize: Int,
    @get:Schema(description = "Collection of results in the page")
    val content: Collection<T>
)
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

package org.eclipse.tractusx.bpdm.gate.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError

@Schema(description = "Paginated collection of results")
data class PageOutputResponse<T>(
    @Schema(description = "Total number of all results in all pages")
    val total: Int,
    @Schema(description = "Value to be used as startAfter in request for next page. Value is only sent if more data exists for a next page.")
    val nextStartAfter: String?,
    @Schema(description = "Collection of results in the page")
    val content: Collection<T>,
    @Schema(description = "Number of entries in the page that have been omitted due to being invalid (error or still pending)")
    val invalidEntries: Int,
    @Schema(description = "External ids of the entries which are still pending")
    val pending: Collection<String>,
    @Schema(description = "Infos about the entries with errors")
    val errors: Collection<ErrorInfo<BusinessPartnerOutputError>>,
)
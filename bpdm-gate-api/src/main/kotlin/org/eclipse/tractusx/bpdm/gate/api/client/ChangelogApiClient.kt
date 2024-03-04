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

package org.eclipse.tractusx.bpdm.gate.api.client

import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateChangelogApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange("/api/catena")
interface ChangelogApiClient : GateChangelogApi {
    @PostExchange("/input/changelog/search")
    override fun getInputChangelog(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody searchRequest: ChangelogSearchRequest
    ): PageChangeLogResponse<ChangelogGateResponse>

    @PostExchange("/output/changelog/search")
    override fun getOutputChangelog(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestBody searchRequest: ChangelogSearchRequest
    ): PageChangeLogResponse<ChangelogGateResponse>

}
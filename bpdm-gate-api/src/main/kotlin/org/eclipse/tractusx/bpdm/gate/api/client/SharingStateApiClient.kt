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

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

@HttpExchange("/api/catena/sharing-state")
interface SharingStateApiClient : GateSharingStateApi {
    @GetExchange
    override fun getSharingStates(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @Parameter(description = "Business partner type") @RequestParam(required = false) businessPartnerType: BusinessPartnerType?,
        @Parameter(description = "External IDs") @RequestParam(required = false) externalIds: Collection<String>?
    ): PageDto<SharingStateDto>

    @PostExchange("/ready")
    override fun postSharingStateReady(@RequestBody request: PostSharingStateReadyRequest)

    @PutExchange
    override fun upsertSharingState(@RequestBody request: SharingStateDto)

}
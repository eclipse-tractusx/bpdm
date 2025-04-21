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
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.GateSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import java.time.Instant

@HttpExchange
interface SharingStateApiClient : GateSharingStateApi {
    @GetExchange(value = ApiCommons.SHARING_STATE_PATH_V7)
    override fun getSharingStates(
        @ParameterObject @Valid paginationRequest: PaginationRequest,
        @RequestParam(required = false) externalIds: Collection<String>?,
        @RequestParam(required = false) sharingStateTypes: Collection<SharingStateType>?,
        @RequestParam(required = false) updatedAfter: Instant?,
    ): PageDto<SharingStateDto>

    @PostExchange(value = "${ApiCommons.SHARING_STATE_PATH_V7}/ready")
    override fun postSharingStateReady(@RequestBody request: PostSharingStateReadyRequest)

}
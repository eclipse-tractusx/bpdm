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

package org.eclipse.tractusx.bpdm.pool.api

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.CxMembershipDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.CxMembershipUpdateRequest
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
interface PoolCxMembershipApi {

    @GetMapping(value = [ApiCommons.MEMBERSHIP_BASE_PATH_V6, ApiCommons.MEMBERSHIP_BASE_PATH_V7])
    fun get(
        @ParameterObject searchRequest: CxMembershipSearchRequest,
        @ParameterObject paginationRequest: PaginationRequest
    ): PageDto<CxMembershipDto>


    @PutMapping(value = [ApiCommons.MEMBERSHIP_BASE_PATH_V6, ApiCommons.MEMBERSHIP_BASE_PATH_V7])
    fun put(@RequestBody updateRequest: CxMembershipUpdateRequest)
}
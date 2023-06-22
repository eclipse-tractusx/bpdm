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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.config.SecurityConfigProperties
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateChangelogApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangeLogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogResponse
import org.eclipse.tractusx.bpdm.gate.config.GateSecurityConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.ChangelogService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class ChangelogController(
    private val changelogService: ChangelogService,
    val gateSecurityConfigProperties: GateSecurityConfigProperties,
    val securityConfigProperties: SecurityConfigProperties
) : GateChangelogApi {

    @PreAuthorize("!@changelogController.securityConfigProperties.enabled || hasAuthority(@changelogController.gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getInputChangelog(
        paginationRequest: PaginationRequest, searchRequest: ChangeLogSearchRequest
    ): PageChangeLogResponse<ChangelogResponse> {
        return changelogService.getChangeLogEntries(searchRequest.externalIds, searchRequest.lsaTypes, searchRequest.fromTime, paginationRequest.page, paginationRequest.size)
    }

    @PreAuthorize("!@changelogController.securityConfigProperties.enabled || hasAuthority(@changelogController.gateSecurityConfigProperties.getReadCompanyOutputDataAsRole())")
    override fun getOutputChangelog(paginationRequest: PaginationRequest,
                                    searchRequest: ChangeLogSearchRequest): PageChangeLogResponse<ChangelogResponse> {
        throw NotImplementedError()
        TODO("Not yet implemented")
    }

}
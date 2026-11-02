/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.controller.v7

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v7.GateChangelogV7Api
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.ChangelogGateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.ChangelogSearchV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.PageChangeLogV7Dto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7ChangelogMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.ChangelogSearchApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class ChangelogController(
    private val changelogSearchApplicationService: ChangelogSearchApplicationService,
    private val apiV7ChangelogMapper: ApiV7ChangelogMapper,
    private val tenantResolver: TenantResolver
) : GateChangelogV7Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_CHANGELOG})")
    override fun getInputChangelog(
        paginationRequest: PaginationRequest, searchRequest: ChangelogSearchV7Request
    ): PageChangeLogV7Dto<ChangelogGateV7Dto> {
        val request = ChangelogSearchRaw(searchRequest.externalIds, searchRequest.timestampAfter)
        val page = changelogSearchApplicationService.search(request, StageType.Input, GoldenRecordType.BusinessPartner, tenantResolver.currentUserBpn(), paginationRequest.toPageRequest())
        return apiV7ChangelogMapper.toPageDto(page)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_CHANGELOG})")
    override fun getOutputChangelog(
        paginationRequest: PaginationRequest,
        searchRequest: ChangelogSearchV7Request
    ): PageChangeLogV7Dto<ChangelogGateV7Dto> {
        val request = ChangelogSearchRaw(searchRequest.externalIds, searchRequest.timestampAfter)
        val page = changelogSearchApplicationService.search(request, StageType.Output, GoldenRecordType.BusinessPartner, tenantResolver.currentUserBpn(), paginationRequest.toPageRequest())
        return apiV7ChangelogMapper.toPageDto(page)
    }

}
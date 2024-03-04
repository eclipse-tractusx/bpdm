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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.GateSiteApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.service.SiteService
import org.eclipse.tractusx.bpdm.gate.util.containsDuplicates
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class SiteController(
    val siteService: SiteService,
    val apiConfigProperties: ApiConfigProperties
) : GateSiteApi {


    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_AUTHORITY})")
    override fun upsertSites(sites: Collection<SiteGateInputRequest>): ResponseEntity<Unit> {
        if (sites.size > apiConfigProperties.upsertLimit || sites.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        siteService.upsertSites(sites)
        return ResponseEntity(HttpStatus.OK)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_AUTHORITY})")
    override fun getSiteByExternalId(externalId: String): SiteGateInputResponse {
        return siteService.getSiteByExternalId(externalId)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_AUTHORITY})")
    override fun getSitesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageDto<SiteGateInputResponse> {
        return siteService.getSites(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_AUTHORITY})")
    override fun getSites(paginationRequest: PaginationRequest): PageDto<SiteGateInputResponse> {
        return siteService.getSites(page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_AUTHORITY})")
    override fun getSitesOutput(paginationRequest: PaginationRequest, externalIds: Collection<String>?): PageDto<SiteGateOutputResponse> {
        return siteService.getSitesOutput(externalIds = externalIds, page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_OUTPUT_AUTHORITY})")
    override fun upsertSitesOutput(sites: Collection<SiteGateOutputRequest>): ResponseEntity<Unit> {
        if (sites.size > apiConfigProperties.upsertLimit || sites.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        siteService.upsertSitesOutput(sites)
        return ResponseEntity(HttpStatus.OK)
    }

}
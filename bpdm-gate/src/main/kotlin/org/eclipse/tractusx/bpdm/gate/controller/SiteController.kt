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

import org.eclipse.tractusx.bpdm.gate.client.service.site.SiteInterface
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.service.SiteService
import org.eclipse.tractusx.bpdm.gate.service.ValidationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SiteController(
    val siteService: SiteService,
    val apiConfigProperties: ApiConfigProperties,
    val validationService: ValidationService
) : SiteInterface {

    override fun upsertSites(sites: Collection<SiteGateInputRequest>): ResponseEntity<Any> {
        if (sites.size > apiConfigProperties.upsertLimit || sites.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        siteService.upsertSites(sites)
        return ResponseEntity(HttpStatus.OK)
    }

    override fun getSiteByExternalId(externalId: String): SiteGateInputResponse {
        return siteService.getSiteByExternalId(externalId)
    }

    override fun getSites(paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<SiteGateInputResponse> {
        return siteService.getSites(paginationRequest.limit, paginationRequest.startAfter)
    }

    override fun getSitesOutput(paginationRequest: PaginationStartAfterRequest, externalIds: Collection<String>?): PageOutputResponse<SiteGateOutput> {
        return siteService.getSitesOutput(externalIds, paginationRequest.limit, paginationRequest.startAfter)
    }

    override fun validateSite(siteInput: SiteGateInputRequest): ValidationResponse {
        return validationService.validate(siteInput)
    }


}
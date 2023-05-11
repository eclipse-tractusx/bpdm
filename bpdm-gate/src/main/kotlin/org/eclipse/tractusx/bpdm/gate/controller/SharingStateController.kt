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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.GateSharingStateApi
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.service.AddressService
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.eclipse.tractusx.bpdm.gate.service.SiteService
import org.springframework.web.bind.annotation.RestController

@RestController
class SharingStateController(
    val legalEntityService: LegalEntityService,
    val siteService: SiteService,
    val addressService: AddressService
) : GateSharingStateApi {

    private val logger = KotlinLogging.logger { }

    override fun getSharingStates(paginationRequest: PaginationRequest, lsaType: LsaType?, externalIds: Collection<String>?): PageResponse<SharingStateDto> {
        // TODO Replace mock implementation using persistence:
        //  For now only the BPN is collected from CDQ input. If there is none, no sharing status is returned.
        //  For now lsaType is required!
        lsaType ?: throw IllegalArgumentException("lsaType is required")
        externalIds ?: throw IllegalArgumentException("externalIds is required")

        val sharingStates = when (lsaType) {
            LsaType.LegalEntity ->
                legalEntityService.getLegalEntities(externalIds = externalIds, limit = paginationRequest.size, startAfter = null).content
                    .filter { it.bpn != null }
                    .map {
                        SharingStateDto(
                            lsaType = lsaType,
                            externalId = it.externalId,
                            bpn = it.bpn,
                            sharingStateType = SharingStateType.Success
                        )
                    }

            LsaType.Site ->
                siteService.getSites(externalIds = externalIds, limit = paginationRequest.size, startAfter = null).content
                    .filter { it.bpn != null }
                    .map {
                        SharingStateDto(
                            lsaType = lsaType,
                            externalId = it.externalId,
                            bpn = it.bpn,
                            sharingStateType = SharingStateType.Success
                        )
                    }

            LsaType.Address ->
                addressService.getAddresses(externalIds = externalIds, limit = paginationRequest.size, startAfter = null).content
                    .filter { it.bpn != null }
                    .map {
                        SharingStateDto(
                            lsaType = lsaType,
                            externalId = it.externalId,
                            bpn = it.bpn,
                            sharingStateType = SharingStateType.Success
                        )
                    }
        }

        // TODO Not yet implemented
        return PageResponse(
            totalElements = sharingStates.size.toLong(),
            totalPages = if (sharingStates.size > 0) 1 else 0,
            page = 0,
            contentSize = sharingStates.size,
            content = sharingStates
        )
    }

    override fun upsertSharingState(request: SharingStateDto) {
        // TODO Not yet implemented
        logger.info { "upsertSharingState() called with $request" }
    }
}

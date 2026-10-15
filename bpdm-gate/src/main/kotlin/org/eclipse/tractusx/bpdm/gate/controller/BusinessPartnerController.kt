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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.GateBusinessPartnerApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.BpdmBusinessPartnerInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound.BusinessPartnerInputRequestMapper
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.BusinessPartnerInputDtoMapper
import org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound.BusinessPartnerInputUpsertErrorMapper
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.BusinessPartnerService
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.input.BusinessPartnerInputUpsertApplicationService
import org.eclipse.tractusx.bpdm.gate.util.getCurrentUserBpn
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class BusinessPartnerController(
    val businessPartnerService: BusinessPartnerService,
    val inputUpsertApplicationService: BusinessPartnerInputUpsertApplicationService,
    val inputRequestMapper: BusinessPartnerInputRequestMapper,
    val inputDtoMapper: BusinessPartnerInputDtoMapper,
    val inputErrorMapper: BusinessPartnerInputUpsertErrorMapper
) : GateBusinessPartnerApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_PARTNER})")
    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputRequest>): ResponseEntity<Collection<BusinessPartnerInputDto>> {
        val requests = businessPartners.map(inputRequestMapper::toUpsertIn)
        return when (val result = inputUpsertApplicationService.upsert(requests, getCurrentUserBpn())) {
            is BusinessPartnerInputUpsertResult.Written -> ResponseEntity.ok(result.inputs.map(inputDtoMapper::toDto))
            is BusinessPartnerInputUpsertResult.Rejected -> throw BpdmBusinessPartnerInputUpsertException(result.errors.map(inputErrorMapper::toMessage))
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_PARTNER})")
    override fun getBusinessPartnersInput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerInputDto> {
        return businessPartnerService.getBusinessPartnersInput(paginationRequest.toPageRequest(), externalIds,  getCurrentUserBpn())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_PARTNER})")
    override fun getBusinessPartnersOutput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerOutputDto> {
        return businessPartnerService.getBusinessPartnersOutput(paginationRequest.toPageRequest(), externalIds,  getCurrentUserBpn())
    }
}

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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.GateBusinessPartnerV6Api
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Dto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.input.BusinessPartnerInputV6Request
import org.eclipse.tractusx.bpdm.gate.api.v6.model.businesspartner.output.BusinessPartnerOutputV6Dto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.controller.TenantResolver
import org.eclipse.tractusx.bpdm.gate.exception.BpdmBusinessPartnerInputUpsertException
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6BusinessPartnerMapper
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6InboundMapper
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.application.BusinessPartnerInputSearchApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.BusinessPartnerInputUpsertApplicationService
import org.eclipse.tractusx.bpdm.gate.service.application.BusinessPartnerOutputSearchApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("businessPartnerControllerLegacy")
class BusinessPartnerController(
    val inputUpsertApplicationService: BusinessPartnerInputUpsertApplicationService,
    val inputSearchApplicationService: BusinessPartnerInputSearchApplicationService,
    val outputSearchApplicationService: BusinessPartnerOutputSearchApplicationService,
    val apiV6InboundMapper: ApiV6InboundMapper,
    val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper,
    val tenantResolver: TenantResolver
) : GateBusinessPartnerV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.WRITE_INPUT_PARTNER})")
    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputV6Request>): ResponseEntity<Collection<BusinessPartnerInputV6Dto>> {
        val requests = businessPartners.map(apiV6InboundMapper::toUpsertIn)
        return when (val result = inputUpsertApplicationService.upsert(requests, tenantResolver.currentUserBpn())) {
            is BusinessPartnerInputUpsertResult.Written -> ResponseEntity.ok(result.inputs.map(apiV6BusinessPartnerMapper::toDto))
            is BusinessPartnerInputUpsertResult.Rejected -> throw BpdmBusinessPartnerInputUpsertException(result.errors.map(apiV6BusinessPartnerMapper::toMessage))
        }
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_INPUT_PARTNER})")
    override fun getBusinessPartnersInput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerInputV6Dto> {
        val request = BusinessPartnerSearchRaw(externalIds)
        return inputSearchApplicationService.search(request, tenantResolver.currentUserBpn(), paginationRequest.toPageRequest())
            .toPageDto(apiV6BusinessPartnerMapper::toDto)
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_PARTNER})")
    override fun getBusinessPartnersOutput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerOutputV6Dto> {
        val request = BusinessPartnerSearchRaw(externalIds)
        return outputSearchApplicationService.search(request, tenantResolver.currentUserBpn(), paginationRequest.toPageRequest())
            .toPageDto(apiV6BusinessPartnerMapper::toDto)
    }
}
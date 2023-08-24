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

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.GateAddressApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.service.AddressService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressService: AddressService,
    private val apiConfigProperties: ApiConfigProperties
) : GateAddressApi {


    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getChangeCompanyInputDataAsRole())")
    override fun upsertAddresses(addresses: Collection<AddressGateInputRequest>): ResponseEntity<Unit> {
        if (addresses.size > apiConfigProperties.upsertLimit || addresses.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        if (addresses.any {
                (it.siteExternalId == null && it.legalEntityExternalId == null) || (it.siteExternalId != null && it.legalEntityExternalId != null)
            }) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        addressService.upsertAddresses(addresses)
        return ResponseEntity(HttpStatus.OK)
    }


    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddressByExternalId(externalId: String): AddressGateInputDto {

        return addressService.getAddressByExternalId(externalId)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddressesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageDto<AddressGateInputDto> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddresses(paginationRequest: PaginationRequest): PageDto<AddressGateInputDto> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getReadCompanyOutputDataAsRole())")
    override fun getAddressesOutput(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<AddressGateOutputDto> {
        return addressService.getAddressesOutput(externalIds = externalIds, page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("hasAuthority(@gateSecurityConfigProperties.getChangeCompanyOutputDataAsRole())")
    override fun upsertAddressesOutput(addresses: Collection<AddressGateOutputRequest>): ResponseEntity<Unit> {
        if (addresses.size > apiConfigProperties.upsertLimit || addresses.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        addressService.upsertOutputAddresses(addresses)
        return ResponseEntity(HttpStatus.OK)
    }

}
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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.GateAddressApi
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.GateSecurityConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.service.AddressService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressService: AddressService,
    private val apiConfigProperties: ApiConfigProperties,
    val gateSecurityConfigProperties: GateSecurityConfigProperties,
    val securityConfigProperties: SecurityConfigProperties
) : GateAddressApi {


    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.getChangeCompanyInputDataAsRole())")
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


    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddressByExternalId(externalId: String): AddressGateInputResponse {
        gateSecurityConfigProperties.getReadCompanyOutputDataAsRole()
        return addressService.getAddressByExternalId(externalId)
    }

    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddressesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageResponse<AddressGateInputResponse> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.gateSecurityConfigProperties.getReadCompanyInputDataAsRole())")
    override fun getAddresses(paginationRequest: PaginationRequest): PageResponse<AddressGateInputResponse> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size)
    }
    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.gateSecurityConfigProperties.getReadCompanyOutputDataAsRole())")
    override fun getAddressesOutput(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageResponse<AddressGateOutputResponse> {
        return addressService.getAddressesOutput(externalIds = externalIds, page = paginationRequest.page, size = paginationRequest.size)
    }

    @PreAuthorize("!@addressController.securityConfigProperties.enabled || hasAuthority(@addressController.gateSecurityConfigProperties.gateSecurityConfigProperties.getChangeCompanyOutputDataAsRole())")
    override fun putAddressesOutput(addresses: Collection<AddressGateOutputRequest>): ResponseEntity<Unit> {
        if (addresses.size > apiConfigProperties.upsertLimit || addresses.map { it.externalId }.containsDuplicates()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        addressService.upsertOutputAddresses(addresses)
        return ResponseEntity(HttpStatus.OK)
    }

}
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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.gate.api.GateAddressApi
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.ValidationResponse
import org.eclipse.tractusx.bpdm.gate.config.ApiConfigProperties
import org.eclipse.tractusx.bpdm.gate.containsDuplicates
import org.eclipse.tractusx.bpdm.gate.service.AddressService
import org.eclipse.tractusx.bpdm.gate.service.ValidationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AddressController(
    private val addressService: AddressService,
    private val apiConfigProperties: ApiConfigProperties,
    private val validationService: ValidationService
) : GateAddressApi {

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

    override fun getAddressByExternalId(externalId: String): AddressGateInputResponse {
        return addressService.getAddressByExternalId(externalId)
    }

    override fun getAddressesByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageResponse<AddressGateInputResponse> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size, externalIds = externalIds)
    }

    override fun getAddresses(paginationRequest: PaginationRequest): PageResponse<AddressGateInputResponse> {
        return addressService.getAddresses(page = paginationRequest.page, size = paginationRequest.size)
    }

    override fun getAddressesOutput(
        paginationRequest: PaginationStartAfterRequest,
        externalIds: Collection<String>?
    ): PageOutputResponse<AddressGateOutput> {
        return addressService.getAddressesOutput(externalIds, paginationRequest.limit, paginationRequest.startAfter)
    }

    override fun validateSite(addressInput: AddressGateInputRequest): ValidationResponse {
        return validationService.validate(addressInput)
    }

}
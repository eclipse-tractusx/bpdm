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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressGateOutputDto
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class AddressService(
    private val addressPersistenceService: AddressPersistenceService,
    private val addressRepository: GateAddressRepository,
) {

    fun getAddresses(page: Int, size: Int, externalIds: Collection<String>? = null): PageDto<AddressGateInputDto> {

        val logisticAddressPage = if (externalIds != null) {
            addressRepository.findByExternalIdInAndStage(externalIds, StageType.Input, PageRequest.of(page, size))
        } else {
            addressRepository.findByStage(StageType.Input, PageRequest.of(page, size))
        }

        return PageDto(
            page = page,
            totalElements = logisticAddressPage.totalElements,
            totalPages = logisticAddressPage.totalPages,
            contentSize = logisticAddressPage.content.size,
            content = toValidLogisticAddresses(logisticAddressPage),
        )
    }

    private fun toValidLogisticAddresses(logisticAddressPage: Page<LogisticAddress>): List<AddressGateInputDto> {
        return logisticAddressPage.content.map { logisticAddress ->
            logisticAddress.toAddressGateInputResponse(logisticAddress)
        }
    }

    fun getAddressByExternalId(externalId: String): AddressGateInputDto {

        val logisticAddress =
            addressRepository.findByExternalIdAndStage(externalId, StageType.Input) ?: throw BpdmNotFoundException("Logistic Address", externalId)

        return logisticAddress.toAddressGateInputResponse(logisticAddress)

    }

    /**
     * Get output addresses by fetching addresses from the database.
     */
    fun getAddressesOutput(externalIds: Collection<String>? = null, page: Int, size: Int): PageDto<AddressGateOutputDto> {

        val logisticAddressPage = if (!externalIds.isNullOrEmpty()) {
            addressRepository.findByExternalIdInAndStage(externalIds, StageType.Output, PageRequest.of(page, size))
        } else {
            addressRepository.findByStage(StageType.Output, PageRequest.of(page, size))
        }

        return PageDto(
            page = page,
            totalElements = logisticAddressPage.totalElements,
            totalPages = logisticAddressPage.totalPages,
            contentSize = logisticAddressPage.content.size,
            content = toValidOutputLogisticAddresses(logisticAddressPage),
        )

    }

    private fun toValidOutputLogisticAddresses(logisticAddressPage: Page<LogisticAddress>): List<AddressGateOutputDto> {
        return logisticAddressPage.content.map { logisticAddress ->
            logisticAddress.toAddressGateOutputResponse(logisticAddress)
        }
    }

    /**
     * Upsert addresses input to the database
     **/
    fun upsertAddresses(addresses: Collection<AddressGateInputRequest>) {
        addressPersistenceService.persistAddressBP(addresses, StageType.Input)
    }

    /**
     * Upsert addresses output to the database
     **/
    fun upsertOutputAddresses(addresses: Collection<AddressGateOutputRequest>) {
        addressPersistenceService.persistOutputAddressBP(addresses, StageType.Output)
    }

}
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

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.AddressGate
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.springframework.stereotype.Service

@Service
class AddressPersistenceService(private val gateAddressRepository: GateAddressRepository) {

    @Transactional
    fun persistAddressBP(addresses: Collection<AddressGateInputRequest>) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->
            val fullAddress = address.toAddressGate()
            addressRecord.find { it.externalId == address.externalId }?.let { existingAddress ->
                updateAddress(existingAddress, address)
                gateAddressRepository.save(existingAddress)
            } ?: run {
                gateAddressRepository.save(fullAddress)
            }
        }
    }

    private fun updateAddress(address: AddressGate, addressRequest: AddressGateInputRequest) {

        address.careOf = addressRequest.address.careOf
        address.country = addressRequest.address.country
        address.version = addressRequest.address.version.toAddressVersionGate()
        address.geoCoordinates = addressRequest.address.geographicCoordinates?.toGeographicCoordinateGate()
        address.externalId = addressRequest.externalId
        address.legalEntityExternalId = addressRequest.legalEntityExternalId.toString()
        address.siteExternalId = addressRequest.siteExternalId.toString()
        address.bpn = addressRequest.bpn.toString()

        address.postCodes.replace(addressRequest.address.postCodes.map { toEntity(it, address) }.toSet())
        address.administrativeAreas.replace(addressRequest.address.administrativeAreas.map { toEntity(it, address) }.toSet())
        address.thoroughfares.replace(addressRequest.address.thoroughfares.map { toEntity(it, address) }.toSet())
        address.localities.replace(addressRequest.address.localities.map { toEntity(it, address) }.toSet())
        address.premises.replace(addressRequest.address.premises.map { toEntity(it, address) }.toSet())
        address.postalDeliveryPoints.replace(addressRequest.address.postalDeliveryPoints.map { toEntity(it, address) }.toSet())
        address.contexts.replace(addressRequest.address.contexts)
        address.types.replace(addressRequest.address.types)

    }

}
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
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.LegalEntity
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.stereotype.Service

@Service
class AddressPersistenceService(
    private val gateAddressRepository: GateAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteEntityRepository: SiteRepository
) {

    @Transactional
    fun persistAddressBP(addresses: Collection<AddressGateInputRequest>) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->

            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalId(it) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalId(it) }

            val fullAddress = address.toAddressGate(legalEntityRecord, siteRecord, OutputInputEnum.Input)

            addressRecord.find { it.externalId == address.externalId && it.dataType == OutputInputEnum.Input }?.let { existingAddress ->
                updateAddress(existingAddress, address, legalEntityRecord, siteRecord)
                gateAddressRepository.save(existingAddress)
            } ?: run {
                gateAddressRepository.save(fullAddress)
            }
        }
    }

    private fun updateAddress(address: LogisticAddress, changeAddress: AddressGateInputRequest, legalEntityRecord: LegalEntity?, siteRecord: Site?) {

        address.name = changeAddress.address.name
        address.externalId = changeAddress.externalId
        address.legalEntity = legalEntityRecord
        address.site = siteRecord
        address.siteExternalId = changeAddress.siteExternalId.toString()
        address.physicalPostalAddress = changeAddress.address.physicalPostalAddress.toPhysicalPostalAddressEntity()
        address.alternativePostalAddress = changeAddress.address.alternativePostalAddress?.toAlternativePostalAddressEntity()

        address.identifiers.replace(changeAddress.address.identifiers.map { toEntityIdentifier(it, address) })
        address.states.replace(changeAddress.address.states.map { toEntityAddress(it, address) })

    }

    @Transactional
    fun persistOutputAddress(addresses: Collection<AddressGateInputRequest>) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->
            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalId(it) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalId(it) }

            val fullAddress = address.toAddressGate(legalEntityRecord, siteRecord, OutputInputEnum.Output)

            addressRecord.find { it.externalId == address.externalId && it.dataType == OutputInputEnum.Output }?.let { existingAddress ->
                updateAddress(existingAddress, address, legalEntityRecord, siteRecord)
                gateAddressRepository.save(existingAddress)
            } ?: run {
                gateAddressRepository.save(fullAddress)
            }
        }
    }

}
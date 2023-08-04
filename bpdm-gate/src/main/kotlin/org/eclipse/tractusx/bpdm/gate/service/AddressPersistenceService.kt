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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.LegalEntity
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AddressPersistenceService(
    private val gateAddressRepository: GateAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteEntityRepository: SiteRepository,
    private val sharingStateService: SharingStateService,
    private val changelogRepository: ChangelogRepository
) {

    @Transactional
    fun persistAddressBP(addresses: Collection<AddressGateInputRequest>, dataType: OutputInputEnum) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->

            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalIdAndDataType(it, dataType) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalIdAndDataType(it, dataType) }

            if (legalEntityRecord == null && siteRecord == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither legal entity record nor site record found for externalID")
            }

            val fullAddress = address.toAddressGate(legalEntityRecord, siteRecord, dataType)

            addressRecord.find { it.externalId == address.externalId && it.dataType == dataType }
                ?.let { existingAddress ->
                    updateAddress(existingAddress, address, legalEntityRecord, siteRecord)
                    gateAddressRepository.save(existingAddress)
                    saveChangelog(address.externalId, ChangelogType.UPDATE, dataType)
                }
                ?: run {
                    gateAddressRepository.save(fullAddress)
                    sharingStateService.upsertSharingState(address.toSharingStateDTO())
                    saveChangelog(address.externalId, ChangelogType.CREATE, dataType)
                }
        }
    }

    private fun updateAddress(address: LogisticAddress, changeAddress: AddressGateInputRequest, legalEntityRecord: LegalEntity?, siteRecord: Site?) {

        address.externalId = changeAddress.externalId
        address.legalEntity = legalEntityRecord
        address.site = siteRecord
        address.physicalPostalAddress = changeAddress.address.physicalPostalAddress.toPhysicalPostalAddressEntity()
        address.alternativePostalAddress = changeAddress.address.alternativePostalAddress?.toAlternativePostalAddressEntity()
        address.identifiers.replace(changeAddress.address.identifiers.distinct().map { toEntityAddressIdentifiers(it, address) })
        address.states.replace(changeAddress.address.states.map { toEntityAddress(it, address) })
        address.nameParts.replace(changeAddress.address.nameParts.map { toNameParts(it, address, null, null) })
        address.roles.replace(changeAddress.address.roles.distinct().map { toRoles(it, null, null, address) })

    }

    @Transactional
    fun persistOutputAddressBP(addresses: Collection<AddressGateOutputRequest>, dataType: OutputInputEnum) {
        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->

            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalIdAndDataType(it, dataType) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalIdAndDataType(it, dataType) }

            if (legalEntityRecord == null && siteRecord == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither legal entity record nor site record found for externalID")
            }

            val fullAddress = address.toAddressGateOutput(legalEntityRecord, siteRecord, dataType)

            addressRecord.find { it.externalId == address.externalId && it.dataType == dataType }
                ?.let { existingAddress ->
                    updateAddressOutput(existingAddress, address, legalEntityRecord, siteRecord)
                    gateAddressRepository.save(existingAddress)
                    saveChangelog(address.externalId, ChangelogType.UPDATE, dataType)
                }
                ?: run {
                    if (addressRecord.find { it.externalId == fullAddress.externalId && it.dataType == OutputInputEnum.Input } == null) {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Input Logistic Address doesn't exist")
                    } else {
                        gateAddressRepository.save(fullAddress)
                        saveChangelog(address.externalId, ChangelogType.CREATE, dataType)
                    }
                }
            sharingStateService.upsertSharingState(address.toSharingStateDTO(SharingStateType.Success))
        }
    }

    private fun updateAddressOutput(address: LogisticAddress, changeAddress: AddressGateOutputRequest, legalEntityRecord: LegalEntity?, siteRecord: Site?) {

        address.bpn = changeAddress.bpn
        address.externalId = changeAddress.externalId
        address.legalEntity = legalEntityRecord
        address.site = siteRecord
        address.physicalPostalAddress = changeAddress.address.physicalPostalAddress.toPhysicalPostalAddressEntity()
        address.alternativePostalAddress = changeAddress.address.alternativePostalAddress?.toAlternativePostalAddressEntity()
        address.identifiers.replace(changeAddress.address.identifiers.distinct().map { toEntityAddressIdentifiers(it, address) })
        address.states.replace(changeAddress.address.states.map { toEntityAddress(it, address) })
        address.nameParts.replace(changeAddress.address.nameParts.map { toNameParts(it, address, null, null) })
        address.roles.replace(changeAddress.address.roles.distinct().map { toRoles(it, null, null, address) })

    }

    private fun saveChangelog(externalId: String, changelogType: ChangelogType, outputInputEnum: OutputInputEnum) {
        changelogRepository.save(ChangelogEntry(externalId, BusinessPartnerType.ADDRESS, changelogType, outputInputEnum))
    }

}
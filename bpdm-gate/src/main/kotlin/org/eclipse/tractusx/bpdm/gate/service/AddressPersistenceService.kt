/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import mu.KLogger
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.AddressGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.gate.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.gate.entity.SiteDb
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
    private val logger: KLogger = KotlinLogging.logger {}
    @Transactional
    fun persistAddressBP(addresses: Collection<AddressGateInputRequest>, dataType: StageType) {

        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->

            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalIdAndStage(it, dataType) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalIdAndStage(it, dataType) }

            if (legalEntityRecord == null && siteRecord == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither legal entity record nor site record found for externalID")
            }

            val fullAddress = address.toAddressGate(legalEntityRecord, siteRecord, dataType)

            addressRecord.find { it.externalId == address.externalId && it.stage == dataType }
                ?.let { existingAddress ->
                    updateAddress(existingAddress, address, legalEntityRecord, siteRecord)
                    gateAddressRepository.save(existingAddress)
                    logger.info { "Address ${existingAddress.bpn} was updated" }
                    saveChangelog(address.externalId, ChangelogType.UPDATE, dataType)
                }
                ?: run {
                    gateAddressRepository.save(fullAddress)
                    logger.info { "Address ${fullAddress.bpn} was created" }
                    saveChangelog(address.externalId, ChangelogType.CREATE, dataType)
                }
        }

        val initRequests = addresses.map { SharingStateService.SharingStateIdentifierDto(it.externalId, BusinessPartnerType.ADDRESS ) }
        sharingStateService.setInitial(initRequests)
    }

    private fun updateAddress(address: LogisticAddressDb, changeAddress: AddressGateInputRequest, legalEntityRecord: LegalEntityDb?, siteRecord: SiteDb?) {

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
    fun persistOutputAddressBP(addresses: Collection<AddressGateOutputRequest>, dataType: StageType) {
        val externalIdColl: MutableCollection<String> = mutableListOf()
        addresses.forEach { externalIdColl.add(it.externalId) }

        val addressRecord = gateAddressRepository.findByExternalIdIn(externalIdColl)

        addresses.forEach { address ->

            val legalEntityRecord = address.legalEntityExternalId?.let { legalEntityRepository.findByExternalIdAndStage(it, dataType) }
            val siteRecord = address.siteExternalId?.let { siteEntityRepository.findByExternalIdAndStage(it, dataType) }

            if (legalEntityRecord == null && siteRecord == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither legal entity record nor site record found for externalID")
            }

            val fullAddress = address.toAddressGateOutput(legalEntityRecord, siteRecord, dataType)

            addressRecord.find { it.externalId == address.externalId && it.stage == dataType }
                ?.let { existingAddress ->
                    updateAddressOutput(existingAddress, address, legalEntityRecord, siteRecord)
                    gateAddressRepository.save(existingAddress)
                    saveChangelog(address.externalId, ChangelogType.UPDATE, dataType)
                }
                ?: run {
                    if (addressRecord.find { it.externalId == fullAddress.externalId && it.stage == StageType.Input } == null) {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Input Logistic Address doesn't exist")
                    } else {
                        gateAddressRepository.save(fullAddress)
                        saveChangelog(address.externalId, ChangelogType.CREATE, dataType)
                    }
                }
        }

        val successRequests = addresses.map {
            SharingStateService.SuccessRequest(
                SharingStateService.SharingStateIdentifierDto(it.externalId, BusinessPartnerType.ADDRESS),
                it.bpn
            )
        }
        sharingStateService.setSuccess(successRequests)
    }

    private fun updateAddressOutput(
        address: LogisticAddressDb,
        changeAddress: AddressGateOutputRequest,
        legalEntityRecord: LegalEntityDb?,
        siteRecord: SiteDb?
    ) {

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

    private fun saveChangelog(externalId: String, changelogType: ChangelogType, stage: StageType) {
        changelogRepository.save(ChangelogEntryDb(externalId, BusinessPartnerType.ADDRESS, changelogType, stage))
    }

}